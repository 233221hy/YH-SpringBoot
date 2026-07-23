package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplNode;
import cn.xfywz.guozespring.service.admin.SlTplNodeService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlTplNodeController {
    @Autowired
    private SlTplNodeService slTplNodeService;
    @PostMapping("/tpl_node_add")
    public Result add(SlTplNode slTplNode) {
        return slTplNodeService.add(slTplNode);
    }
    @PostMapping("/tpl_node_update")
    public Result update(SlTplNode slTplNode) {
        return slTplNodeService.update(slTplNode);
    }
    @GetMapping("/del_tpl_node")
    public Result del(@RequestParam int id) {
        return slTplNodeService.del(id);
    }
}
