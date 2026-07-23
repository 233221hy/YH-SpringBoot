package cn.xfywz.guozespring.service.file;

import cn.xfywz.guozespring.entity.file.AsyncQueryTask;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PracticeReportAsyncService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private PracticeReportAsyncExecutor executor;

    private static final String TASK_REDIS_KEY_PREFIX = "practice_report:export:task:";
    private static final long TASK_EXPIRE_MINUTE = 60;

    // ===================== 创建导出任务 =====================
    public String createExportTask(int schoolId, int courseId, Integer classId, Integer status) throws Exception {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String redisKey = TASK_REDIS_KEY_PREFIX + taskId;

        AsyncQueryTask task = new AsyncQueryTask();
        task.setTaskId(taskId);
        task.setStatus("RUNNING");
        task.setTotal(0);
        task.setCurrent(0);
        redisTemplate.opsForValue().set(redisKey, task, TASK_EXPIRE_MINUTE, TimeUnit.MINUTES);

        // 跨类调用 → 异步一定生效
        executor.execute(taskId, schoolId, courseId, classId);

        return taskId;
    }

    // ===================== 获取任务 =====================
    public AsyncQueryTask getTask(String taskId) {
        return (AsyncQueryTask) redisTemplate.opsForValue().get(TASK_REDIS_KEY_PREFIX + taskId);
    }
}