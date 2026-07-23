package cn.xfywz.guozespring.service.student;

import cn.xfywz.guozespring.entity.dto.ResetPasswordDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.vo.StudentStats;
import cn.xfywz.guozespring.util.Result;

public interface YeeStudentMangerService {

    StudentStats getStudentStats(int schoolId, long studentId) throws Exception;
    Result studentInfoUpdate(String Authorization, YeeStudent yeeStudent) throws Exception;
    YeeStudent getInfo(String Authorization) throws Exception;
    Result updatePhone(String mobile, String Authorization) throws Exception;
    Result updateEmail(String mobile,String Authorization) throws Exception;
    Result infoUpdatePassword(String oldPassword, String newPassword, String authorization) throws Exception;

    Result forgetPassword(ResetPasswordDTO dto, int schoolId);
}
