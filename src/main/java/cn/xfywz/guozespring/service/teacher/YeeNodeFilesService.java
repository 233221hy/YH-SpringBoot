package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeNodeFiles;
import cn.xfywz.guozespring.util.Result;

public interface YeeNodeFilesService {
    Result selectByNodeId(Integer schoolId, long nodeId);
    Result add(YeeNodeFiles nodeFiles);
    Result update(YeeNodeFiles nodeFiles);
    Result delete(Integer schoolId, long id);
}
