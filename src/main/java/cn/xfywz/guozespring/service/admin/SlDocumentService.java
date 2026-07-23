package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlDocument;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlDocumentService {
    Result add(SlDocument slDocument);
    Result delete(Integer id);
    Result update(SlDocument slDocument);
    Result list(Integer pageNum, Integer pageSize);
    Result selectLike(String name);
}
