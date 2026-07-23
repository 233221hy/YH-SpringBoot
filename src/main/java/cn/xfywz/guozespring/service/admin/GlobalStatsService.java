package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.vo.GlobalStatsVO;
import org.springframework.stereotype.Service;

public interface GlobalStatsService {

    /**
     * 获取所有学校的统计数据汇总
     */
    GlobalStatsVO getGlobalStats();
}
