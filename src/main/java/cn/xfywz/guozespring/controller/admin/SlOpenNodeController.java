package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNode;
import cn.xfywz.guozespring.service.admin.SlOpenNodeService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlOpenNodeController {
    @Autowired
    private SlOpenNodeService slOpenNodeService;
    @PostMapping("/open_node_add")
    public Result add(SlOpenNode slOpenNode) {
        return slOpenNodeService.add(slOpenNode);
    }
    @PostMapping("/open_node_update")
    public Result update(SlOpenNode slOpenNode) {
        return slOpenNodeService.update(slOpenNode);
    }
    @GetMapping("/del_open_node")
    public Result delete(@RequestParam Integer id) {
        return slOpenNodeService.delete(id);
    }
}
