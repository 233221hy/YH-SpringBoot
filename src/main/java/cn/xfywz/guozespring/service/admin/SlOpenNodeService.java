package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNode;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlOpenNodeService {
    Result add(SlOpenNode slOpenNode);
    Result update(SlOpenNode slOpenNode);
    Result delete(Integer id);
}
