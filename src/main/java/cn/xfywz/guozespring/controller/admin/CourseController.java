package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.dto.ResetPasswordDTO;
import cn.xfywz.guozespring.entity.dto.SlOpenCourseQueryDTO;
import cn.xfywz.guozespring.entity.dto.TopicWithCoursesDTO;
import cn.xfywz.guozespring.service.admin.*;
import cn.xfywz.guozespring.service.student.YeeStudentMangerService;
import cn.xfywz.guozespring.service.teacher.SlSchBannerService;
import cn.xfywz.guozespring.service.teacher.YeeStudentService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.SchoolDomainResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/course")
public class CourseController {
    @Autowired
    private CourseService courseService;
    @Autowired
    private CategroyService categroyService;
    @Autowired
    private SlOpenCourseService slOpenCourseService;
    @Autowired
    private SlDocumentService slDocumentService;
    @Autowired
    private SlSettingsService slSettingsService;
    @Autowired
    private SlSchBannerService slSchBannerService;
    @Autowired
    private SlSchoolThematicService slSchoolThematicService;
    @Autowired
    private YeeStudentMangerService yeeStudentMangerService;
    @Autowired
    private SchoolDomainResolver schoolDomainResolver;

    @GetMapping("/selectAll")
    public Result AllList(int PageSize, int PageNum) throws Exception{
        try {
            return courseService.AllList(PageSize,PageNum);
        }catch (Exception e){
            PageSize=100;
            PageNum=1;
            return courseService.AllList(PageSize,PageNum);
        }
    }
    @GetMapping("/selectLike")
    public Result selectLike(@RequestParam String name,
                             @RequestParam(defaultValue = "10") int PageSize,
                             @RequestParam(defaultValue = "1") int PageNum) {
        name="%"+name+"%";
        return courseService.selectLikeSlTplCourse(PageSize,PageNum,name);
    }
    @GetMapping("/selectTplAll")
    public Result selectOcAll(int PageSize,int PageNum) {
        return courseService.selectSlTplCourseAll(PageSize,PageNum);
    }
    @GetMapping("/selectLikeOc")
    public Result selectLikeOc(@RequestParam String name, int PageSize, int PageNum) {
        name="%"+name+"%";
        return courseService.selectLikeByName(PageSize,PageNum,name);
    }
    @GetMapping("/selectHotCourse")
    public Result selectHotCourse() {
        return courseService.selectSlHomeHotCourse();
    }
    @GetMapping("/selectCourseNode")
    public Result selectCourseNode(@RequestParam int id) {
        return courseService.selectCourseNode(id);
    }
    @GetMapping("/selectCourseId")
    public Result selectCourseId(@RequestParam int id) {
        return courseService.selectSlTplCourseById(id);
    }
    @GetMapping("/selectOpenCourseId")
    public Result selectOpenCourseId(@RequestParam int id) {
        return courseService.selectOpenCourseById(id);
    }
    @GetMapping("/selectTplCourseNode")
    public Result selectTplCourseNode(@RequestParam int id) {
        return courseService.selectTplNode(id);
    }
    @GetMapping("/selectCategroy")
    public Result selectCategroy() {
        return categroyService.selectAll();
    }

    @GetMapping("/selectTplCourseByCateId")
    public Result selectTplCourseByCateId(@RequestParam int cateId) {
        return courseService.selectTplCourseByCateId(cateId);
    }

    @GetMapping("/selectOpenCourseByCateId")
    public Result selectOpenCourseByCateId(@RequestParam int cateId) {
        return courseService.selectOpenCourseByCateId(cateId);
    }

    @GetMapping("/selectdomain")
    public Result selectDomain(String domain) {
        return courseService.selectDomain(domain);
    }

    //查询对应学校的公开课
    @PostMapping("/open_course_list")
    public Result openCourseList(@RequestBody SlOpenCourseQueryDTO queryDTO,
                                 HttpServletRequest request){
        int schoolId = schoolDomainResolver.getSchoolIdByHost(request);
        return slOpenCourseService.openCourseList(queryDTO, schoolId);
    }

    @GetMapping("/document_list")
    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum){
        return slDocumentService.list(PageNum,PageSize);
    }

    @GetMapping("/settings_info")
    public Result info(){
        return slSettingsService.info();
    }

    //查询轮播图列表
    @GetMapping("/sl_sch_banner_list")
    public Result list(@RequestParam int pageSize,
                       @RequestParam int pageNum,
                       HttpServletRequest request) throws Exception {
        int schoolId = schoolDomainResolver.getSchoolIdByHost(request);
        return slSchBannerService.list(pageSize, pageNum, schoolId);
    }

    /**
     * 根据学校域名获取专题列表（含关联课程）
     *
     * @param domain 学校域名，例如 "example.xfywz.cn"
     * @return Result<List<TopicWithCoursesDTO>>
     */
    @GetMapping("/school_topics")
    public Result getTopicsByDomain(@RequestParam String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return Result.error("域名不能为空");
        }
        return slSchoolThematicService.getThematicsWithCoursesByDomain(domain);
    }

    //学生忘记密码
    @PostMapping("/stu_forget_pwd")
    public Result forgetPassword(@RequestBody ResetPasswordDTO dto, HttpServletRequest request) {
        int schoolId = schoolDomainResolver.getSchoolIdByHost(request);
        return yeeStudentMangerService.forgetPassword(dto, schoolId);
    }
}
