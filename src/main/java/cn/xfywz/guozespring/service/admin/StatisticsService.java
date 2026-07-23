package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.dto.StatisticsQueryParam;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;


public interface StatisticsService {
    Result getStatistics(StatisticsQueryParam param) throws Exception;
    
    void exportStatistics(StatisticsQueryParam param, HttpServletResponse response) throws Exception;

    Result getStatisticsMore(StatisticsQueryParam param) throws Exception;
}