package cn.xfywz.guozespring.service.admin;
import cn.xfywz.guozespring.entity.mhmain.SlManage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public interface UserLoginService{
    Map login(SlManage slManage, HttpServletRequest request) throws Exception;
    void loginOut(String auth) throws Exception;
}
