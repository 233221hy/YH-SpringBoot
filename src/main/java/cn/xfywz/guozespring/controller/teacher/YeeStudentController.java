package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.dto.YeeStudentQueryDTO;
import cn.xfywz.guozespring.service.teacher.YeeStudentService;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequireAuth
@Validated
@RestController
@RequestMapping("/school")
public class YeeStudentController {

    @Autowired
    private YeeStudentService yeeStudentService;

    // 学生列表
    @PostMapping("/yee_student_list")
    public Result list(@RequestBody YeeStudentQueryDTO queryDTO) throws Exception {
        return yeeStudentService.selectAll(queryDTO);
    }

    // 学生详情
    @GetMapping("/yee_student_info")
    public Result info(@RequestParam int schoolId,
                       @RequestParam long id) throws Exception {
        return yeeStudentService.selectById(schoolId, id);
    }

    // 新增学生
    @PostMapping("/yee_student_add")
    public Result add(@Valid @RequestBody YeeStudent student) throws Exception {
        yeeStudentService.add(student);
        return Result.success();
    }

    // 修改学生
    @PostMapping("/yee_student_update")
    public Result update(@RequestBody YeeStudent student) throws Exception {
        yeeStudentService.update(student);
        return Result.success();
    }

    // 删除学生
    @GetMapping("/yee_student_delete")
    public Result delete(@RequestParam Long id,
                         @RequestParam int schoolId) throws Exception {
        yeeStudentService.delete(id, schoolId);
        return Result.success();
    }

    // 重置密码
    @PostMapping("/yee_stu_pwd_reset")
    public Result passwordReset(@RequestParam int schoolId,
                                @RequestParam List<String> number) throws Exception {
        yeeStudentService.passwordReset(schoolId, number);
        return Result.success();
    }

    // 批量导出数据
    @PostMapping("/yee_stu_export")
    public void exportData(@RequestBody YeeStudentQueryDTO queryDTO,
                           HttpServletResponse response) throws Exception {
        yeeStudentService.exportData(queryDTO, response);
    }

    // 批量导入
    @PostMapping("/yee_stu_import")
    public Result importData(@RequestParam int schoolId,
                             @RequestParam("file") MultipartFile file) throws Exception {
        return yeeStudentService.importData(schoolId, file);
    }
}