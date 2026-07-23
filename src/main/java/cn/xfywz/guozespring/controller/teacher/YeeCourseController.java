package cn.xfywz.guozespring.controller.teacher;


import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeCourse;
import cn.xfywz.guozespring.entity.vo.LikeYeeCourse;
import cn.xfywz.guozespring.entity.dto.YeeCourseQueryParam;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.service.teacher.YeeCourseService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
public class YeeCourseController {
    @Autowired
    private YeeCourseService yeeCourseService;
    @GetMapping("/yee_course_list")
    public Result selectAll(@RequestParam Integer schoolId, @RequestParam Integer pageSize, @RequestParam Integer pageNum, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeCourseService.selectAll(schoolId, pageSize, pageNum, Authorization);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_course_add")
    public Result add(YeeCourse yeeCourse,@RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization,(int)yeeCourse.getSchoolId())){
            return yeeCourseService.add(yeeCourse);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_course_update")
    public Result update(YeeCourse yeeCourse,@RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization,(int)yeeCourse.getSchoolId())){
            return yeeCourseService.update(yeeCourse);
        }else return Result.error("非法访问");
    }
    @GetMapping("/yee_course_delete")
    public Result delete(int schoolId, int id, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeCourseService.deleteById(schoolId, id);
        }else return Result.error("非法访问");
    }

    @PostMapping("/yee_course_like")
    public Result like(LikeYeeCourse likeYeeCourse,@RequestParam Integer pageSize, @RequestParam Integer pageNum, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, likeYeeCourse.getSchoolId())){
            return yeeCourseService.like(likeYeeCourse,pageSize,pageNum,Authorization);
        }else return Result.error("非法访问");
    }

    /**
     * 课程模板导入
     */
    @PostMapping("/course_template_import")
    public Result courseTemplateImport(YeeCourse yeeCourse,
                       @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeCourse.getSchoolId())){
            return yeeCourseService.courseTemplateImport(yeeCourse);
        }else return Result.error("非法访问");
    }
    
    @PostMapping("/yee_course_list_with_conditions")
    public Result selectAllWithConditions(@RequestBody YeeCourseQueryParam param, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, param.getSchoolId())){
            Integer dataAuth = AuthTokenUtil.extractDataAuth(Authorization);
            if (dataAuth.equals(DataAuth.ALL.getValue())){
                return yeeCourseService.selectAllWithConditions(param);
            } else {
                throw new Exception("无权限");
            }
        }else return Result.error("非法访问");
    }

    @PostMapping("/yee_course_export")
    public void exportCourseData(@RequestBody YeeCourseQueryParam param, @RequestHeader String Authorization, HttpServletResponse response) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, param.getSchoolId())){
            Integer dataAuth = AuthTokenUtil.extractDataAuth(Authorization);
            if (dataAuth.equals(DataAuth.ALL.getValue())){
                yeeCourseService.exportCourseData(param, response);
            } else {
                throw new Exception("无权限");
            }
        } else {
            response.setStatus(403);
            response.getWriter().write("非法访问");
        }
    }

    @PostMapping("/yee_course_student_enrollment_export")
    public void exportCourseStudentEnrollmentData(@RequestBody YeeCourseQueryParam param, @RequestHeader String Authorization, HttpServletResponse response) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, param.getSchoolId())){
            Integer dataAuth = AuthTokenUtil.extractDataAuth(Authorization);
            yeeCourseService.exportCourseStudentEnrollmentData(param, response);
        } else {
            response.setStatus(403);
            response.getWriter().write("非法访问");
        }
    }



    @GetMapping("/yee_course_content")
    public Result selectCourseContent(
            @RequestParam(required = true) Integer schoolId,
            @RequestParam(required = true) Integer id,
            @RequestParam(required = true) Integer classId,
            @RequestHeader String Authorization) throws Exception {

        // 手动校验非空（因为 int 不能为 null，但 Integer 可以）
        if (schoolId == null || schoolId <= 0) {
            return Result.error("schoolId 不能为空且必须大于0");
        }
        if (id == null || id <= 0) {
            return Result.error("courseId 不能为空且必须大于0");
        }

        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeCourseService.selectCourseContent(schoolId, id, classId);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_course_copy")
    public Result yeeCourseCopy(
            YeeCourse yeeCourse,
            @RequestHeader(name = "Authorization", required = true) String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeCourse.getSchoolId())){
            return yeeCourseService.copyCourse(yeeCourse);
        }else return Result.error("非法访问");
    }
}
