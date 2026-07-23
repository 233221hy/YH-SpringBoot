package cn.xfywz.guozespring.controller.teacher;


import cn.xfywz.guozespring.entity.file.AsyncQueryTask;

import cn.xfywz.guozespring.service.file.PracticeReportAsyncService;
import cn.xfywz.guozespring.service.teacher.YeePracticeReportService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/school")
public class YeePracticeReportController {

    @Resource
    private YeePracticeReportService yeePracticeReportService;

    @Resource
    private PracticeReportAsyncService practiceReportAsyncService;

    private final RateLimiter exportRateLimiter = RateLimiter.create(5.0);

    @GetMapping("/practice_report_stats")
    public Result stats(@RequestParam int schoolId,
                        @RequestParam int courseId,
                        @RequestHeader String Authorization) throws Exception {
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法访问");
        }
        return yeePracticeReportService.stats(schoolId, courseId);
    }

    @GetMapping("/practice_report_list")
    public Result list(@RequestParam int schoolId,
                       @RequestParam int courseId,
                       @RequestParam(required = false) Integer classId,
                       @RequestParam(required = false) String studentNumber,
                       @RequestParam(required = false) String studentName,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestHeader String Authorization) throws Exception {
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法访问");
        }
        return yeePracticeReportService.list(schoolId, courseId, classId,
                studentNumber, studentName, status, pageNum, pageSize);
    }

    @GetMapping("/practice_report_detail")
    public Result detail(@RequestParam int schoolId,
                         @RequestParam long reportId,
                         @RequestHeader String Authorization) throws Exception {
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法访问");
        }
        return yeePracticeReportService.detail(schoolId, reportId);
    }

    @PostMapping("/practice_report_review")
    public Result review(@RequestParam int schoolId,
                         @RequestParam long reportId,
                         @RequestParam String result,
                         @RequestParam(required = false) String remark,
                         @RequestHeader String Authorization) throws Exception {
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法访问");
        }
        // 从 JWT 提取审核人 ID（不再依赖 Sa-Token 会话，避免 Sa-Token 过期导致请求失败）
        long reviewerId = AuthTokenUtil.extractUserId(Authorization);
        return yeePracticeReportService.review(schoolId, reportId, result,
                reviewerId, remark != null ? remark : "");
    }

    @PostMapping("/practice_report_export_create")
    public Result exportCreate(@RequestParam int schoolId,
                               @RequestParam int courseId,
                               @RequestParam(required = false) Integer classId,
                               @RequestParam(required = false) Integer status,
                               @RequestHeader String Authorization) throws Exception {
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法访问");
        }
        if (!exportRateLimiter.tryAcquire()) {
            return Result.error("导出请求过于频繁，请稍后再试");
        }
        String taskId = practiceReportAsyncService.createExportTask(
                schoolId, courseId, classId, status);
        return Result.success("任务已开始生成，请稍候", taskId);
    }

    @PostMapping("/practice_report_export_result")
    public Result exportResult(@RequestParam String taskId) {
        AsyncQueryTask task = practiceReportAsyncService.getTask(taskId);
        if (task == null) {
            return Result.error("任务不存在或已过期");
        }
        int current = task.getCurrent() != null ? task.getCurrent() : 0;
        int total = task.getTotal() != null ? task.getTotal() : 0;
        String status = task.getStatus();
        String msg;

        if ("RUNNING".equals(status)) {
            msg = total > 0 ? "正在生成" : "正在准备数据...";
        } else if ("FAILED".equals(status)) {
            msg = task.getErrorMsg();
        } else if ("SUCCESS".equals(status)) {
            msg = "导出完成";
        } else {
            msg = "任务状态异常";
        }

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("status", status);
        resMap.put("current", current);
        resMap.put("total", total);
        return Result.success(msg, (Object) resMap);
    }

    @GetMapping("/practice_report_export_download")
    public void exportDownload(@RequestParam String taskId,
                               HttpServletResponse response) throws Exception {
        AsyncQueryTask task = practiceReportAsyncService.getTask(taskId);
        if (task == null || !"SUCCESS".equals(task.getStatus())) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":500,\"msg\":\"文件未生成或已过期\"}");
            return;
        }

        File file = new File(task.getFilePath());
        response.setContentType("application/zip");
        String downloadName = task.getFileName() != null ? task.getFileName() : "实践报告_导出.zip";
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(downloadName, "UTF-8"));

        try (InputStream in = Files.newInputStream(file.toPath());
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }

        if (file.exists()) {
            file.delete();
        }
    }
}