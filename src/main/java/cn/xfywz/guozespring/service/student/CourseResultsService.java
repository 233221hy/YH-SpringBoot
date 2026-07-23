package cn.xfywz.guozespring.service.student;

import cn.xfywz.guozespring.entity.dto.CourseResultsQueryDTO;
import cn.xfywz.guozespring.util.Result;

public interface CourseResultsService {

    Result courseResults(CourseResultsQueryDTO param);

}
