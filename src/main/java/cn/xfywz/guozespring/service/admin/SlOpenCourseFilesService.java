package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenCourseFiles;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlOpenCourseFilesService {
    Result List(Integer id);
    Result add(SlOpenCourseFiles slOpenCourseFiles);
    Result update(SlOpenCourseFiles slOpenCourseFiles);
    Result del(Integer id);
}
