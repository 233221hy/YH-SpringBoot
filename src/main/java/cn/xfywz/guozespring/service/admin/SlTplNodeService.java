package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplNode;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlTplNodeService {
    Result add(SlTplNode slTplNode);
    Result update(SlTplNode slTplNode);
    Result del(int id);
}
