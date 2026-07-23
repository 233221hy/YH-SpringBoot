package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlRoleService {
    Result selectAll(int schoolId ,int PageSize,int PageNum);
    Result add(SlRole slRole) throws Exception;
    Result update(SlRole slRole);
    Result delete(Integer id);
    Result getNode();
}
