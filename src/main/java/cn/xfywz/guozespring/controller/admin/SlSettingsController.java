package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlSettings;
import cn.xfywz.guozespring.service.admin.SlSettingsService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage")
public class SlSettingsController {
    @Autowired
    private SlSettingsService slSettingsService;
    @GetMapping("/settings_info")
    public Result info(){
        return slSettingsService.info();
    }
    @PostMapping("/settings_update")
    public Result update(SlSettings slSettings){
        return slSettingsService.update(slSettings);
    }
}
