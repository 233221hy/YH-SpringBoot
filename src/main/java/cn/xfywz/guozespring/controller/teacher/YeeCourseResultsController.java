package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.YeeCourseResultsQueryDTO;
import cn.xfywz.guozespring.service.teacher.YeeCourseResultsService;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCourseResultsController {
    @Autowired
    private YeeCourseResultsService yeeCourseResultsService;
    
    @PostMapping("/yee_course_results_list")
    public Result list(@RequestBody YeeCourseResultsQueryDTO queryDTO) throws Exception {
        return yeeCourseResultsService.list(queryDTO);
    }

    
    /**
     * 批量计算班级所有学生成绩
     * 根据配置的计分规则，计算指定班级内所有学生的各项成绩
     */
    @GetMapping("/yee_course_results_calculate")
    public Result calculateScore(@RequestParam Integer schoolId,
                                 @RequestParam long courseId,
                                 @RequestParam long classId) {
        try {
            yeeCourseResultsService.calculateScore(schoolId, courseId, classId);

            return Result.success("成绩计算任务已提交，正在后台处理中，请稍后刷新页面查看最新成绩");

        } catch (Exception e) {
            return Result.error("成绩计算提交失败" + e.getMessage());
        }
    }

    /**
     * 导出指定班级的课程成绩为Excel
     */
    @PostMapping("/yee_course_results_export")
    public void exportResults(@RequestBody YeeCourseResultsQueryDTO queryDTO,
                              HttpServletResponse response) throws Exception {
        yeeCourseResultsService.exportResults(queryDTO, response);
    }

    /**
     * 导出额外分数成绩为Excel
     */
    @PostMapping("/yee_course_results_export_extra")
    public void exportExtraScore(@RequestBody YeeCourseResultsQueryDTO queryDTO,
                                 HttpServletResponse  response) throws Exception {
        yeeCourseResultsService.exportExtraScore(queryDTO, response);
    }

    /**
     * 导入额外分数
     */
    @PostMapping("/yee_course_results_import_extra")
    public Result importExtraScore(@RequestParam Integer schoolId,
                                   @RequestPart("file") MultipartFile file,
                                   @RequestParam String courseId) throws Exception{
        return yeeCourseResultsService.importExtraScore(schoolId, file, courseId);
    }
}
