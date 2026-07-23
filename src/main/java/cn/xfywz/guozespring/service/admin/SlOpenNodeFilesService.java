package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNodeFiles;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlOpenNodeFilesService {
    Result add(SlOpenNodeFiles slOpenNodeFiles);
    Result update(SlOpenNodeFiles slOpenNodeFiles);
    Result delete(Integer id);
    Result selectById(Integer id);
}
