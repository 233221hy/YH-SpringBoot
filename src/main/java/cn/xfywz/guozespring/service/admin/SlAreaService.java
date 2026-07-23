package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlAreaService {
    Result list();
    Result select(Integer pid);
}
