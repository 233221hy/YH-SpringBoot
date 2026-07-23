package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.YeeCollegeQueryDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeCollege;
import cn.xfywz.guozespring.service.teacher.YeeClassesService;
import cn.xfywz.guozespring.service.teacher.YeeCollegeService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCollegeController {
    @Autowired
    private YeeCollegeService yeeCollegeService;
    @Autowired
    private YeeClassesService yeeClassesService;

    @PostMapping("/yee_college_add")
    public Result add(YeeCollege yeeCollege) throws Exception {
        yeeCollegeService.add(yeeCollege);
        return Result.success("添加成功");
    }
    @PostMapping("/yee_college_update")
    public Result update(YeeCollege yeeCollege) throws Exception {
        return yeeCollegeService.update(yeeCollege);
    }
    @GetMapping("/yee_college_delete")
    public Result delete(int schoolId, int id) throws Exception {
        // 检查学院下是否存在班级
        if (yeeClassesService.hasClassesByCollegeId(schoolId,id)) {
            return Result.error("请先删除该学院下的班级");
        }

        return yeeCollegeService.delete(schoolId, id);
    }
    @PostMapping("/yee_college_list")
    public Result selectAll(@RequestBody YeeCollegeQueryDTO queryDTO) throws Exception {
        return yeeCollegeService.selectAll(queryDTO);
    }

    //根据id查学院
    @GetMapping("/yee_college_select_by_id")
    public Result selectById(int schoolId, long id) throws Exception {
        return yeeCollegeService.selectById(schoolId, id);
    }
}
