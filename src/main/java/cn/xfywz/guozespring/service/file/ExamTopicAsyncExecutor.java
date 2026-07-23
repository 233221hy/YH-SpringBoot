package cn.xfywz.guozespring.service.file;

import cn.xfywz.guozespring.entity.file.AsyncQueryTask;
import cn.xfywz.guozespring.service.teacher.impl.YeeExamServiceImpl;
import cn.xfywz.guozespring.util.Result;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExamTopicAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(ExamTopicAsyncExecutor.class);


    @Resource
    private YeeExamServiceImpl yeeExamService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TASK_REDIS_KEY_PREFIX = "exam:export:task:";
    private static final long TASK_EXPIRE_MINUTE = 60;
    private static final String EXPORT_TMP_DIR = "/tmp/exam_export/";
    private static final int FETCH_SIZE = 50;
    private static final int TOPIC_BATCH_SIZE = 50;

    @Async("exportExecutor")
    public void execute(
            String taskId, int schoolId, Integer courseId, Integer examId,
            String title, Integer classId,
            Integer subState, Integer reviewState, Integer scoredState,
            String operateUserId
    ) {
        String redisKey = TASK_REDIS_KEY_PREFIX + taskId;
        File tmpDir = new File(EXPORT_TMP_DIR);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        File zipFile = new File(tmpDir, taskId + ".zip");
        long startTime = System.currentTimeMillis();
        final long MAX_TIME = 10 * 60 * 1000;

        // 🔴 最外层兜底：任何异常都必须打日志+更新失败状态
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 8192);
             ZipOutputStream zipOut = new ZipOutputStream(bos)) {

            // 1. 一次性获取所有有效学生完整数据（含成绩合并、状态过滤），
            //    后续分批直接使用，不再重复查询数据库。
            List<Map<String, Object>> allStudents = yeeExamService.getAllValidStudentsForExport(
                    schoolId, courseId, examId, title,
                    classId, subState, reviewState, scoredState
            );

            if (allStudents.isEmpty()) {
                throw new RuntimeException("没有可导出的数据，请检查筛选条件");
            }

            int total = allStudents.size();
            AtomicInteger current = new AtomicInteger(0);

            // 2. 初始化任务状态
            AsyncQueryTask initTask = new AsyncQueryTask();
            initTask.setTaskId(taskId);
            initTask.setStatus("RUNNING");
            initTask.setTotal(total);
            initTask.setCurrent(0);
            redisTemplate.opsForValue().set(redisKey, initTask, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);

            // 3. 分批（直接分学生数据，不再只分ID）
            List<List<Map<String, Object>>> studentBatches = partitionList(allStudents, FETCH_SIZE);

            // 4. 逐批处理
            for (int batchIdx = 0; batchIdx < studentBatches.size(); batchIdx++) {
                List<Map<String, Object>> studentList = studentBatches.get(batchIdx);

                // 超时检查
                if (System.currentTimeMillis() - startTime > MAX_TIME) {
                    log.error("[导出任务-{}] 执行超时（>10分钟），强制终止", taskId);
                    throw new RuntimeException("导出超时");
                }

                // 4.1 补题目信息（studentList 已含完整学生+成绩数据，无需再查）
                try {
                    enrichStudentWithTopics(schoolId, courseId, examId, studentList);
                } catch (Exception e) {
                    throw e;
                }

                // 4.2 逐个生成PDF并写入ZIP
                for (Map<String, Object> student : studentList) {
                    String name = (String) student.get("name");
                    String number = (String) student.get("number");
                    Integer userId = (Integer) student.get("userId");
                    try {
                        ByteArrayOutputStream pdfBaos = yeeExamService.generateStudentExamPdf(student);
                        zipOut.putNextEntry(new ZipEntry(name + "_" + number + ".pdf"));
                        zipOut.write(pdfBaos.toByteArray());
                        zipOut.closeEntry();

                        int now = current.incrementAndGet();
                        updateProgress(redisKey, now);

                    } catch (Exception e) {
                        // 单个学生失败只打日志，不终止整个任务
                        log.error("[导出任务-{}] 学生PDF生成失败：userId={},name={},number={}",
                                taskId, userId, name, number, e);
                    }
                }

            }

            // 5. 全部成功
            AsyncQueryTask finishTask = new AsyncQueryTask();
            finishTask.setTaskId(taskId);
            finishTask.setStatus("SUCCESS");
            finishTask.setFileName("考试答卷_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".zip");
            finishTask.setTotal(total);
            finishTask.setCurrent(current.get());
            finishTask.setFilePath(zipFile.getAbsolutePath());
            redisTemplate.opsForValue().set(redisKey, finishTask, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);


        } catch (Throwable e) {
            // 任何环节异常：日志+堆栈+标记失败
            log.error("[导出任务-{}] ❌ 导出任务失败：{}", taskId, e.getMessage(), e);
            AsyncQueryTask failTask = new AsyncQueryTask();
            failTask.setTaskId(taskId);
            failTask.setStatus("FAILED");
            failTask.setErrorMsg("导出失败：" + e.getMessage());
            redisTemplate.opsForValue().set(redisKey, failTask, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);
        }
    }


    private void enrichStudentWithTopics(int schoolId, Integer courseId, Integer examId, List<Map<String, Object>> pageList) {
        List<Integer> userIds = pageList.stream()
                .map(student -> (Integer) student.get("userId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        List<List<Integer>> batches = partitionList(userIds, TOPIC_BATCH_SIZE);
        List<Map<String, Object>> allTopicData = new ArrayList<>();

        for (List<Integer> batch : batches) {
            try {
                Result batchResult = yeeExamService.selectWorkRecordConsultBatch(schoolId, examId, courseId, batch);
                List<Map<String, Object>> batchData = (List<Map<String, Object>>) batchResult.getData();
                if (batchData != null) {
                    allTopicData.addAll(batchData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Map<Integer, Map<String, Object>> topicMap = allTopicData.stream()
                .collect(Collectors.toMap(
                        item -> (Integer) item.get("userId"),
                        item -> item,
                        (existing, replacement) -> existing
                ));

        for (Map<String, Object> student : pageList) {
            Integer userId = (Integer) student.get("userId");
            Map<String, Object> topic = topicMap.get(userId);
            if (topic != null) {
                student.putAll(topic);
            } else {
            }
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            result.add(new ArrayList<>(list.subList(i, end)));
        }
        return result;
    }

    private void updateProgress(String redisKey, int current) {
        try {
            AsyncQueryTask task = (AsyncQueryTask) redisTemplate.opsForValue().get(redisKey);
            if (task != null) {
                task.setCurrent(current);
                redisTemplate.opsForValue().set(redisKey, task, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }
}