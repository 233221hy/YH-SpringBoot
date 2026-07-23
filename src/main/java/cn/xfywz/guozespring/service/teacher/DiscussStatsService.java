package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.DiscussStatsDTO;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

public interface DiscussStatsService {
    Result list(DiscussStatsDTO param);
    // 讨论详情列表：按用户维度展示其发表或回复
    Result detailList(DiscussStatsDTO param);

    Result discussDetail(int schoolId, long discussId, int pageNum, int pageSize);

    // 导出学生讨论统计
    void exportData(DiscussStatsDTO param, HttpServletResponse response);
}
