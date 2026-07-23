package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenCourseCluster;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.util.Result;

public interface SlOpenCourseClusterService {
    Result selectAll(int PageSize, int PageNum, TplCourseLike condition);
    Result add(SlOpenCourseCluster slOpenCourseCluster);
    Result del(int id);
    Result update(SlOpenCourseCluster slOpenCourseCluster);
}
