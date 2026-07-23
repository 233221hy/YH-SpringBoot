package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeCategory;
import cn.xfywz.guozespring.service.teacher.YeeCategoryService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCategoryController {
    @Autowired
    private YeeCategoryService yeeCategoryService;
    @GetMapping("/yee_category_list")
    public Result selectAll(int schoolId , @RequestParam(required = false) Integer allow) throws Exception{
        return yeeCategoryService.selectAll(schoolId,allow);
    }
    @PostMapping("/yee_category_add")
    public Result add(YeeCategory yeeCategory) throws Exception{
        return yeeCategoryService.add(yeeCategory);
    }
    @PostMapping("/yee_category_update")
    public Result update(YeeCategory yeeCategory) throws Exception{
        return yeeCategoryService.update(yeeCategory);
    }
    @GetMapping("/yee_category_delete")
    public Result delete(int schoolId, int id) throws Exception{
        return yeeCategoryService.delete(schoolId, id);
    }
}
