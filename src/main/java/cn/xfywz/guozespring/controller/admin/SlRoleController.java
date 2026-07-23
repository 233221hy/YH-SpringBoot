package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.service.admin.SlRoleService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlRoleController {
    @Autowired
    private SlRoleService slRoleService;
    @GetMapping("/role_list")
    public Result selectAll(@RequestParam int schoolId,@RequestParam int PageSize, @RequestParam int PageNum) {
        return slRoleService.selectAll(schoolId,PageSize, PageNum);
    }
    @PostMapping("/role_add")
    public Result add(SlRole slRole) throws Exception {
        return slRoleService.add(slRole);
    }
    @PostMapping("/role_update")
    public Result update(SlRole slRole) {
        return slRoleService.update(slRole);
    }
    @GetMapping("/role_del")
    public Result delete(@RequestParam Integer id) {
        return slRoleService.delete(id);
    }
    @GetMapping("/role_get_node")
    public Result getNode() {
        return slRoleService.getNode();
    }
}
