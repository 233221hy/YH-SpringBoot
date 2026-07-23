package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlLiveVideo;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlLiveVideoService {
    Result add(SlLiveVideo slLiveVideo);
    Result delete(Integer id);
    Result update(SlLiveVideo slLiveVideo);
    Result list(Integer pageNum, Integer pageSize);
    Result selectLike(String title);
    Result listAll(int pageSize, int pageNum);
}
