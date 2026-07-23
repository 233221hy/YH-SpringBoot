package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface CategroyService {
    public Result selectAll();
    Result categroyList();
}
