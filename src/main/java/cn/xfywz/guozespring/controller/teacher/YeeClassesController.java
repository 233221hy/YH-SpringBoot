package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeClasses;
import cn.xfywz.guozespring.entity.dto.YeeClassesQueryParam;
import cn.xfywz.guozespring.service.teacher.YeeClassesService;
import cn.xfywz.guozespring.service.teacher.YeeStudentService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeClassesController {

    @Autowired
    private YeeClassesService yeeClassesService;
    @Autowired
    private YeeStudentService yeeStudentService;

    // 班级列表
    @GetMapping("/yee_class_list")
    public Result list(@RequestParam int pageSize,
                       @RequestParam int pageNum,
                       @RequestParam int schoolId) throws Exception {
        return yeeClassesService.selectAll(schoolId, pageNum, pageSize);
    }

    // 班级详情
    @GetMapping("/yee_class_info")
    public Result info(@RequestParam int schoolId,
                       @RequestParam long id) throws Exception {
        return yeeClassesService.selectById(schoolId, id);
    }

    // 新增班级
    @PostMapping("/yee_class_add")
    public Result add(@RequestBody YeeClasses classes) throws Exception {
        yeeClassesService.add(classes);
        return Result.success();
    }

    // 修改班级
    @PostMapping("/yee_class_update")
    public Result update(@RequestBody YeeClasses classes) throws Exception {
        yeeClassesService.update(classes);
        return Result.success();
    }

    // 删除班级
    @GetMapping("/yee_class_delete")
    public Result delete(@RequestParam Long id,
                         @RequestParam int schoolId) throws Exception {
        // 检查学院下是否存在班级
        if (yeeStudentService.hasStudentByClassId(schoolId,id)) {
            return Result.error("请先删除该班级下的学生");
        }

        yeeClassesService.delete(id, schoolId);
        return Result.success();
    }

    // 条件查询
    @PostMapping("/yee_class_search")
    public Result search(@RequestBody YeeClassesQueryParam param) throws Exception {
        return yeeClassesService.searchByCondition(param);
    }

    // 锁定/解锁
    @GetMapping("/yee_class_lock")
    public Result lock(@RequestParam Long id,
                       @RequestParam int schoolId) throws Exception {
        yeeClassesService.lock(id, schoolId);
        return Result.success();
    }
}
