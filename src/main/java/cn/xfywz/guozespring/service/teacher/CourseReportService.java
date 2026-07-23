package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.vo.CourseReport;
import cn.xfywz.guozespring.util.Result;

public interface CourseReportService {
    Result overview(CourseReport param);
    
    Result activity(CourseReport param);
}
