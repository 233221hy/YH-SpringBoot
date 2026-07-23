package cn.xfywz.guozespring.service.student;
import cn.xfywz.guozespring.util.Result;

public interface LoginService {
    Result login(String number, String password, int schoolId) throws Exception;
    Result logout(String number);
}
