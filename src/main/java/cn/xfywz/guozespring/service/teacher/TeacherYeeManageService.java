package cn.xfywz.guozespring.service.teacher;


import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.dto.YeeManageQueryParam;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public interface TeacherYeeManageService {
    YeeManage getInfo(String Authorization) throws Exception;

    Result infoUpdate(String Authorization,YeeManage yeeManage ) throws Exception;

    Result selectAll(YeeManageQueryParam param) throws Exception;

    Result add(YeeManage yeeManage) throws Exception;

    void update(YeeManage yeeManage) throws Exception;

    void delete(Long id, int schoolId, String account) throws Exception;

    Result infoUpdatePassword(String oldPassword, String newPassword, String authorization) throws Exception;

    void lock(Long id, int schoolId) throws Exception;

    Result searchByCondition(YeeManageQueryParam param) throws Exception;

    Result searchById(Long id, int schoolId);

    // 导入教师表格（附加学院ID）
    Result importData(int schoolId, Long collegeId, MultipartFile file) throws Exception;

    // 重置教师账号密码（支持批量）
    Result passwordReset(int schoolId, List<Long> id) throws Exception;
}
