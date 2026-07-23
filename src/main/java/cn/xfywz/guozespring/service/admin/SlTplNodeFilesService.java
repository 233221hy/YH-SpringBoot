package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplNodeFiles;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlTplNodeFilesService {
    Result selectAll(int pageSize, int pageNum,int courseId);

    Result add(SlTplNodeFiles slTplNodeFiles);

    Result del(int id);

    Result update(SlTplNodeFiles slTplNodeFiles);
}
