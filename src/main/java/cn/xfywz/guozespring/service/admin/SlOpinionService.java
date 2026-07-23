package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpinion;
import cn.xfywz.guozespring.entity.vo.SlOpinionLike;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlOpinionService {
    Result add(SlOpinion slOpinion);
    Result delete(Integer id);
    Result list(Integer pageNum, Integer pageSize);
    Result like(SlOpinionLike slOpinionLike);
}
