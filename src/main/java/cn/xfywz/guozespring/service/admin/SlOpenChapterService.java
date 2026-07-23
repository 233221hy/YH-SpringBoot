package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenChapter;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlOpenChapterService {
    Result selectId(int id);
    Result add(SlOpenChapter slOpenChapter);
    Result del(int id);
    Result update(SlOpenChapter slOpenChapter);
}
