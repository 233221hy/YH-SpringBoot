package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.service.admin.SlAreaService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage")
public class SlAreaController {
    @Autowired
    private SlAreaService slAreaService;
    @GetMapping("/area_list")
    public Result list(){
        return slAreaService.list();
    }
    @GetMapping("/area_select")
    public Result select(Integer pid){
        return slAreaService.select(pid);
    }
}
