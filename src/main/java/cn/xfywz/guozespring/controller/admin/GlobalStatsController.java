package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.vo.GlobalStatsVO;
import cn.xfywz.guozespring.service.admin.GlobalStatsService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage/global")
public class GlobalStatsController {

    @Autowired
    private GlobalStatsService GlobalStatsService;

    /**
     * 获取全局统计数据
     */
    @GetMapping("/data_summary")
    public Result getGlobalStatistics() {
        GlobalStatsVO data = GlobalStatsService.getGlobalStats();
        return Result.success(data);
    }
}
