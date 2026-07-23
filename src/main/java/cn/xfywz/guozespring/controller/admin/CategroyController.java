package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.service.admin.CategroyService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage")
public class CategroyController {
    @Autowired
    private CategroyService categroyService;
    @RequestMapping("/category_list")
    public Result categroyList() {
        return categroyService.categroyList();
    }
}
