package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.vo.LoginLog;
import cn.xfywz.guozespring.util.Result;

public interface LoginLogService {
    Result studentList(LoginLog param);
    Result teacherList(LoginLog param);
}
