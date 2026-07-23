package cn.xfywz.guozespring.service.file;

import cn.xfywz.guozespring.entity.file.AsyncQueryTask;
import cn.xfywz.guozespring.service.teacher.YeePracticeReportService;
import cn.xfywz.guozespring.util.PdfExportUtil;
import cn.xfywz.guozespring.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PracticeReportAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(PracticeReportAsyncExecutor.class);

    @Resource
    private YeePracticeReportService yeePracticeReportService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TASK_REDIS_KEY_PREFIX = "practice_report:export:task:";
    private static final long TASK_EXPIRE_MINUTE = 60;
    private static final String EXPORT_TMP_DIR = "/tmp/practice_report_export/";
    private static final int MAX_RETRY = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async("practiceReportExportExecutor")
    public void execute(String taskId, int schoolId, int courseId, Integer classId) {
        String redisKey = TASK_REDIS_KEY_PREFIX + taskId;
        File tmpDir = new File(EXPORT_TMP_DIR);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        File zipFile = new File(tmpDir, taskId + ".zip");
        long startTime = System.currentTimeMillis();
        final long MAX_TIME = 10 * 60 * 1000;

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 8192);
             ZipOutputStream zipOut = new ZipOutputStream(bos)) {

            // 1. 只导出状态为"已通过"(status=2)的报告
            Result result = yeePracticeReportService.allReportsForExport(schoolId, courseId, classId, 2);
            List<Map<String, Object>> reportList = (List<Map<String, Object>>) result.getData();

            if (reportList == null || reportList.isEmpty()) {
                throw new RuntimeException("没有可导出的数据，请检查筛选条件");
            }

            int total = reportList.size();
            AtomicInteger current = new AtomicInteger(0);

            // 2. 初始化任务状态
            AsyncQueryTask initTask = new AsyncQueryTask();
            initTask.setTaskId(taskId);
            initTask.setStatus("RUNNING");
            initTask.setTotal(total);
            initTask.setCurrent(0);
            redisTemplate.opsForValue().set(redisKey, initTask, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);

            // 3. 逐个生成PDF并写入ZIP
            for (Map<String, Object> report : reportList) {
                // 超时检查
                if (System.currentTimeMillis() - startTime > MAX_TIME) {
                    log.error("[实践报告导出任务-{}] 执行超时（>10分钟），强制终止", taskId);
                    throw new RuntimeException("导出超时");
                }

                String name = (String) report.get("name");
                String number = (String) report.get("number");
                Long studentId = (Long) report.get("studentId");

                // 4. 生成PDF（含重试机制）
                byte[] pdfBytes = generatePdfWithRetry(report, taskId, studentId, name, number);

                // 5. 写入ZIP
                zipOut.putNextEntry(new ZipEntry(number + "_" + name + ".pdf"));
                zipOut.write(pdfBytes);
                zipOut.closeEntry();

                int now = current.incrementAndGet();
                updateProgress(redisKey, now);
            }

            // 6. 全部成功
            AsyncQueryTask finishTask = new AsyncQueryTask();
            finishTask.setTaskId(taskId);
            finishTask.setStatus("SUCCESS");
            finishTask.setTotal(total);
            finishTask.setCurrent(current.get());
            finishTask.setFilePath(zipFile.getAbsolutePath());
            finishTask.setFileName("实践报告_导出.zip");
            redisTemplate.opsForValue().set(redisKey, finishTask, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);

            log.info("[实践报告导出任务-{}] 导出成功，共{}份，ZIP: {}", taskId, total, zipFile.getAbsolutePath());

        } catch (Throwable e) {
            log.error("[实践报告导出任务-{}] 导出任务失败：{}", taskId, e.getMessage(), e);
            AsyncQueryTask failTask = new AsyncQueryTask();
            failTask.setTaskId(taskId);
            failTask.setStatus("FAILED");
            failTask.setErrorMsg("导出失败：" + e.getMessage());
            redisTemplate.opsForValue().set(redisKey, failTask, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);
        }
    }

    /**
     * 生成单个学生PDF，失败时最多重试 MAX_RETRY 次
     */
    private byte[] generatePdfWithRetry(Map<String, Object> report, String taskId,
                                        Long studentId, String name, String number) throws Exception {
        Exception lastException = null;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                return generateSinglePdf(report);
            } catch (Exception e) {
                lastException = e;
                log.warn("[实践报告导出任务-{}] 学生PDF生成失败(第{}次/共{}次)：studentId={},name={},number={}",
                        taskId, retry + 1, MAX_RETRY, studentId, name, number, e);
                if (retry < MAX_RETRY - 1) {
                    Thread.sleep(1000);
                }
            }
        }
        // 所有重试均失败 → 抛异常终止整个任务
        throw new RuntimeException("学生PDF生成失败（已重试" + MAX_RETRY + "次）：" +
                "studentId=" + studentId + ",name=" + name + ",number=" + number, lastException);
    }

    /**
     * 为单个学生生成PDF字节数组
     */
    private byte[] generateSinglePdf(Map<String, Object> report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = PdfExportUtil.initDocument(baos);
        try {
            // 标题
            String title = (String) report.get("title");
            PdfExportUtil.addTitle(document, title != null ? title : "实践报告");

            // 信息表：姓名、学号、班级、提交时间
            String name = (String) report.get("name");
            String number = (String) report.get("number");
            String className = (String) report.get("className");
            Timestamp submitTime = (Timestamp) report.get("submitTime");
            String submitTimeStr = submitTime != null ?
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(submitTime) : "";

            Map<String, String> infoMap = new LinkedHashMap<>();
            infoMap.put("姓名", name != null ? name : "");
            infoMap.put("学号", number != null ? number : "");
            infoMap.put("班级", className != null ? className : "");
            infoMap.put("提交时间", submitTimeStr);
            infoMap.put("通过状态", "已通过");
            PdfExportUtil.addInfoTable(document, infoMap);

            // 报告内容
            String content = (String) report.get("content");
            PdfExportUtil.addContent(document, content != null ? content : "");

            // 附件列表
            String filesJson = (String) report.get("files");
            List<String> urls = parseFileUrls(filesJson);
            if (urls != null && !urls.isEmpty()) {
                PdfExportUtil.addAttachments(document, urls);
            }

        } finally {
            PdfExportUtil.closeDocument(document);
        }
        return baos.toByteArray();
    }

    /**
     * 解析附件JSON字符串为URL列表
     */
    private List<String> parseFileUrls(String filesJson) {
        if (filesJson == null || filesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(filesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("解析附件JSON失败：{}", filesJson, e);
            return Collections.emptyList();
        }
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