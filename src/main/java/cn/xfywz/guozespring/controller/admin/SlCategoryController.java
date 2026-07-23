package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlCategory;
import cn.xfywz.guozespring.service.admin.SlCategoryService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage")
public class SlCategoryController {
    @Autowired
    private SlCategoryService slCategoryService;
    @PostMapping("/sl_category_add")
    public Result add(SlCategory slCategory)
    {
        return slCategoryService.add(slCategory);
    }
    @GetMapping("/sl_category_del")
    public Result delete(Integer id)
    {
        return slCategoryService.delete(id);
    }
    @PostMapping("/sl_category_update")
    public Result update(SlCategory slCategory){
        return slCategoryService.update(slCategory);
    }

}
