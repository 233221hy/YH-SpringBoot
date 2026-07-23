package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseStudent;
import cn.xfywz.guozespring.service.teacher.YeeCourseStudentService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCourseStudentController {
    @Autowired
    private YeeCourseStudentService yeeCourseStudentService;

    @GetMapping("/yee_courseStudent_selectAll")
    public Result selectAll(@RequestParam Integer schoolId,
                           @RequestParam long courseId,
                           @RequestParam long classId,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize) throws Exception {
        return yeeCourseStudentService.selectAll(schoolId, courseId, classId, pageNum, pageSize);
    }

    @PostMapping("/yee_courseStudent_add")
    public Result add(YeeCourseStudent yeeCourseStudent) throws Exception {
        return yeeCourseStudentService.add(yeeCourseStudent);
    }

    @PostMapping("/yee_courseStudent_batchAdd")
    public Result batchAdd(@RequestParam List<Long> studentIds,
                           @RequestParam long courseId,
                           @RequestParam long classId,
                           @RequestParam Integer schoolId) throws Exception {
        return yeeCourseStudentService.batchAdd(studentIds,courseId,classId,schoolId);
    }

    @PostMapping("/yee_courseStudent_update")
    public Result update(YeeCourseStudent yeeCourseStudent) throws Exception {
        return yeeCourseStudentService.update(yeeCourseStudent);
    }

    @PostMapping("/yee_courseStudent_delete")
    public Result delete(
            @RequestBody Map<String, Object> params) throws Exception {

        // 安全获取数字类型，避免 Integer 转 Long 异常
        Integer schoolId = (Integer) params.get("schoolId");
        long courseId = Long.parseLong(params.get("courseId").toString());
        long classId = Long.parseLong(params.get("classId").toString());
        List<Long> studentIds = (List<Long>) params.get("studentIds");

        return yeeCourseStudentService.delete(schoolId, courseId, classId, studentIds);
    }

    // 批量导入(选课学生)
    @PostMapping("/yee_courseStudent_import")
    public Result importCourseStudent(@RequestParam int schoolId,
                                      @RequestParam int courseId,
                                      @RequestParam int classId,
                                    @RequestParam("file") MultipartFile file) throws Exception {
        return yeeCourseStudentService.importCourseStudent(schoolId,courseId,classId,file);
    }

    // 条件查询已选课学生列表
    @GetMapping("/yee_courseStudent_like")
    public Result courseStudentLike(@RequestParam Integer schoolId,
                                    @RequestParam long courseId,
                                    @RequestParam long classId,
                                    @RequestParam(required = false, defaultValue = "") String name,
                                    @RequestParam(required = false, defaultValue = "") String number,
                                    @RequestParam(required = false, defaultValue = "") String idCard,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) throws Exception {
        return yeeCourseStudentService.courseStudentLike(schoolId,courseId,classId,name,number,idCard,pageNum,pageSize);
    }

    // 条件查询所有选课学生列表（标记已选课程type：0-未选该课程、1-已选该课程且是本班级、2-已选该课程其他班级）
    @GetMapping("/yee_Student_type")
    public Result getAllStudentsWithCourseType(@RequestParam Integer schoolId,
                                    @RequestParam long courseId,
                                    @RequestParam long teaClassId,
                                    @RequestParam(required = false, defaultValue = "0") long classId,
                                    @RequestParam(required = false, defaultValue = "") String name,
                                    @RequestParam(required = false, defaultValue = "") String number,
                                    @RequestParam(required = false, defaultValue = "") String idCard,
                                    @RequestParam(required = false, defaultValue = "") String join,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) throws Exception {
        return yeeCourseStudentService.getAllStudentsWithCourseType(schoolId,courseId,classId, teaClassId, name,number,idCard,join,pageNum,pageSize);
    }

    @GetMapping("/courseStudent_templates_download")
    public ResponseEntity<byte[]> templatesDownload(
            @RequestParam Integer schoolId) throws Exception {

        // 使用 try-with-resources 确保流关闭
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("templates/student_import_template.xls")) {

            if (inputStream == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Template file not found in classpath.".getBytes(StandardCharsets.UTF_8));
            }

            byte[] fileBytes = inputStream.readAllBytes();

            // 设置中文文件名
            String filename = "导入选课学生模板.xls";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20"); // 避免空格被转为 +

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", encodedFilename); // Spring 会自动处理编码

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read template file.".getBytes(StandardCharsets.UTF_8));
        }
    }

}