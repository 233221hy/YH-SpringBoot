package cn.xfywz.guozespring.controller.student;


import cn.xfywz.guozespring.entity.mhsch.YeeChapter;
import cn.xfywz.guozespring.service.student.YeeStudentCourseService;
import cn.xfywz.guozespring.service.teacher.YeeAnnouncementService;
import cn.xfywz.guozespring.service.teacher.YeeChapterService;
import cn.xfywz.guozespring.service.teacher.YeeNodeService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class YeeStudentCourseController {
    @Autowired
    private YeeStudentCourseService yeeStudentCourseService;
    @Autowired
    private YeeChapterService yeeChapterService;
    @Autowired
    private YeeNodeService yeeNodeService;
    @Autowired
    private YeeAnnouncementService yeeAnnouncementService;

    //获取课程所有章信息
    @PostMapping("/yee_chapter_select")
    public Result selectAll(YeeChapter yeeChapter, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeChapter.getSchoolId())){
            return yeeChapterService.selectCourse(yeeChapter);
        }else return Result.error("非法请求");
    }

    //获取课程所有节信息
//    @GetMapping("/yee_node_select")
//    public Result select(@RequestParam Integer schoolId,
//                         @RequestParam long chapterId,
//                         @RequestHeader String Authorization) throws Exception {
//        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
//            return yeeNodeService.selectByCourseId(schoolId, chapterId);
//        }else return Result.error("非法访问");
//    }
    //获取课程所有节信息
    @GetMapping("/yee_node_select")
    public Result select(@RequestParam Integer schoolId,
                         @RequestParam long courseId,
                         @RequestParam int studentId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeNodeService.getCourseChapterTreeForStudent(schoolId, courseId, studentId);
        }else return Result.error("非法访问");
    }

    // 获取课程所有章节信息
    @GetMapping("/yee_all_node_chapter")
    public Result selectAllNodesByCourseId(@RequestParam Integer schoolId,
                                           @RequestParam long courseId,
                                           @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeChapterService.getCourseChapterTreeWithExams(schoolId, courseId);
        }else return Result.error("非法访问");
    }

    //获取课程所有公告信息
    @GetMapping("/yee_announcement_selectAll")
    public Result selectAll(@RequestParam Integer schoolId,
                            @RequestParam long courseId,
                            @RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeAnnouncementService.selectAll(schoolId, courseId, pageNum, pageSize);
        } else {
            return Result.error("非法访问");
        }
    }

    //获取我的课程信息列表（0：全部课程；1：已结束）
    @GetMapping("/yee_my_course_list")
    public Result selectAll(@RequestParam Integer schoolId,
                            @RequestParam Integer studentId,
                            @RequestParam Integer type,
                            @RequestParam Integer pageSize,
                            @RequestParam Integer pageNum,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeStudentCourseService.selectList(schoolId,studentId,type, pageSize, pageNum);
        }else return Result.error("非法访问");
    }

    //课程详情
    @GetMapping("/yee_my_course_detail")
    public Result detail(@RequestParam int schoolId,
                         @RequestParam int courseId,
                         @RequestParam int studentId,
                         @RequestHeader String Authorization) throws Exception {
        if (Authorization != null) {
            return yeeStudentCourseService.selectById(schoolId,courseId,studentId);
        } else return Result.error("非法访问");
    }


}
