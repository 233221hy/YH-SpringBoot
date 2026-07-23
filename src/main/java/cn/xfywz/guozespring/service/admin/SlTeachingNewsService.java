package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTeachingNews;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlTeachingNewsService {
    Result add(SlTeachingNews slTeachingNews);
    Result delete(Integer id);
    Result update(SlTeachingNews slTeachingNews);
    Result list(Integer pageNum, Integer pageSize);
    Result selectLike(String title);
    Result listAll(Integer pageNum, Integer pageSize);


}
