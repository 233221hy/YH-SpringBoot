package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.dto.StatisticsQueryParam;
import cn.xfywz.guozespring.service.admin.StatisticsService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @PostMapping("/get_statistics")
    public Result getStatistics(@RequestBody StatisticsQueryParam param, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, param.getSchoolId())) {
            return statisticsService.getStatistics(param);
        } else {
            return Result.error("非法访问");
        }
    }
    
    @PostMapping("/export_statistics")
    public void exportStatistics(@RequestBody StatisticsQueryParam param, @RequestHeader String Authorization, HttpServletResponse response) throws Exception {
        try {
            if (AuthTokenUtil.verifyToken(Authorization, param.getSchoolId())) {
                statisticsService.exportStatistics(param, response);
            } else {
                response.setStatus(403);
                response.getWriter().write("非法访问");
                response.flushBuffer();
            }
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("导出失败: " + e.getMessage());
            response.flushBuffer();
        }
    }

    @PostMapping("/get_statistics_more")
    public Result getStatisticsMore(@RequestBody StatisticsQueryParam param, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, param.getSchoolId())) {
            return statisticsService.getStatisticsMore(param);
        } else {
            return Result.error("非法访问");
        }
    }
}