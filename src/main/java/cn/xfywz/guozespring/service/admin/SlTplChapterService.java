package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplChapter;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlTplChapterService {

    Result add(SlTplChapter slTplChapter);

    Result update(SlTplChapter slTplChapter);

    Result del(int id);
}
