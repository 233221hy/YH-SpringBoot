package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlManageService{
    SlManage info(String Authorization) throws Exception;

    String infoUpdate(SlManage slManage, String authorization) throws Exception;

    Object infoUpdatePassword(String oldPassword, String newPassword, String authorization) throws Exception;

    Result selectAll(int PageSize, int PageNum);

    Result deleteById(int id);

    Result addManage(SlManage slManage);

    Result updateManage(SlManage slManage);

    Result selectById(int id);

    Result selectLikeName(String name);

    Result selectLikeAccount(String account);

    Result manageIsLock(int id);
}
