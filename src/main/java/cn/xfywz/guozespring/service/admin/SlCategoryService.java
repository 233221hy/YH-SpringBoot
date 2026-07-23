package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlCategory;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlCategoryService {
    Result add(SlCategory slCategory);
    Result update(SlCategory slCategory);
    Result delete(Integer id);
}
