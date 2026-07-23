package cn.xfywz.guozespring.controller.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.service.admin.SlSchoolService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlSchoolController {
    @Autowired
    private SlSchoolService slSchoolService;
    @GetMapping("/school_list")
    public Result selectAll(){
        return slSchoolService.selectAll();
    }
    @SaIgnore
    @GetMapping("/school_list_noLogin")
    public Result selectAllNoLogin(){
        return slSchoolService.selectAll();
    }
    @PostMapping("/school_add")
    public Result add(SlSchool slSchool){
        return slSchoolService.add(slSchool);
    }
    @PostMapping("/school_update")
    public Result update(SlSchool slSchool){
        return slSchoolService.update(slSchool);
    }
    @GetMapping("/school_del")
    public Result delete(@RequestParam Integer id){
        return slSchoolService.delete(id);
    }
}
