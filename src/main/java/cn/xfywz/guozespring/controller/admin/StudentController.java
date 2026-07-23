package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.vo.ListID;
import cn.xfywz.guozespring.entity.vo.StuLike;
import cn.xfywz.guozespring.service.admin.StudentService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class StudentController {
    @Autowired
    private StudentService studentService;
    @GetMapping("/stu_list")
    public Result selectAll(@RequestParam int id,@RequestParam int PageSize, @RequestParam int PageNum) throws Exception {
        return studentService.selectAll(PageSize, PageNum, id);
    }

    @GetMapping("/stu_select")
    public Result select(@RequestParam int schoolId,@RequestParam int id) throws Exception {
        return studentService.selectById(schoolId,id);
    }

    @GetMapping("/stu_password_random")
    public Result passwordRandom(@RequestParam int schoolId, @RequestParam int id) throws Exception {
        return studentService.passwordRandom(schoolId,id);
    }
    @PostMapping("/stu_password_reset")
    public Result passwordReset(ListID id) throws Exception {
        return studentService.passwordReset(id.getSchoolId(), id.getId());
    }
    @PostMapping("/like_student")
    public Result selectLike(StuLike like) throws Exception {
        return studentService.selectLikeName(like);
    }

}
