package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeNodeDiscuss;
import cn.xfywz.guozespring.entity.dto.DiscussStatsDTO;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

public interface NodeDiscussStatsService {

    Result list(DiscussStatsDTO param);

    Result detailList(DiscussStatsDTO param);

    void exportData(DiscussStatsDTO param, HttpServletResponse response);
}
