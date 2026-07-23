package cn.xfywz.guozespring.controller.teacher;


import cn.xfywz.guozespring.service.teacher.OnlineUserStatisticsService;
import cn.xfywz.guozespring.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class OnlineUserStatisticsController {

    private final OnlineUserStatisticsService onlineUserStatisticsService;

    // 按学校 + 按小时统计
    @GetMapping("/online/today/hour/school")
    public Result getTodayOnlineBySchool(@RequestParam Long schoolId) {
        if (schoolId == null || schoolId <= 0) {
            return Result.error("学校ID不能为空");
        }
        Map<String, Map<String, Integer>> data = onlineUserStatisticsService.statisticsTodayOnlineBySchool(schoolId);
        return Result.success("统计成功", data);
    }

    /**
     * 【新增】获取当天 按20分钟 在线人数
     */
    @GetMapping("/online/today/20min")
    public Map<String, Map<String, Integer>> getTodayOnlineBy20Min() {
        return onlineUserStatisticsService.statisticsTodayOnlineUserBy20Min();
    }

    /**
     * 当前在线总人数
     */
    @GetMapping("/online/current")
    public Map<String, Integer> getCurrentOnlineTotal() {
        return onlineUserStatisticsService.getCurrentOnlineTotal();
    }

}
