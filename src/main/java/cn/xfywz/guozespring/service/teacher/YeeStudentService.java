package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.dto.YeeStudentQueryDTO;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface YeeStudentService {
    Result selectAll(YeeStudentQueryDTO queryDTO) throws Exception;

    Result selectById(int schoolId, long id) throws Exception;

    void add(YeeStudent student) throws Exception;

    Result update(YeeStudent student) throws Exception;

    void delete(Long id, int schoolId) throws Exception;

    void passwordReset(int schoolId, List<String> number);

    // 批量导出选中学生，返回下载URL
    void exportData(YeeStudentQueryDTO queryDTO, HttpServletResponse response);
    
    // 批量导入学生文件
    Result importData(int schoolId, MultipartFile file);

    boolean hasStudentByClassId(int schoolId, Long classId);
}