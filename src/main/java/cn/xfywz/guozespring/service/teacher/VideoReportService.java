package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.vo.VideoReport;
import cn.xfywz.guozespring.util.Result;

public interface VideoReportService {
    /**
     * 视频报表总览：在某课程内，按班级（可选）筛选，返回课程学习进度与发帖相关统计。
     */
    Result overview(VideoReport param) throws Exception;
}
