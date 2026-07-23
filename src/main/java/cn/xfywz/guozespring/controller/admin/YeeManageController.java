package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.dto.YeeManageQueryParam;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.vo.ListID;
import cn.xfywz.guozespring.entity.vo.YeeManageLike;
import cn.xfywz.guozespring.service.admin.YeeManageService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage")
public class YeeManageController {
    @Autowired
    private YeeManageService yeeManageService;
    @GetMapping("/yee_list")
    public Result selectAll(YeeManageQueryParam param) throws Exception {
        return yeeManageService.selectAll(param);
    }

    @GetMapping("/yee_select")
    public Result select(int schoolId, int id) throws Exception {
        return yeeManageService.selectById(schoolId,id);
    }
    @GetMapping("/yee_delete")
    public Result delete(int schoolId, int id) throws Exception {
        return yeeManageService.deleteById(schoolId,id);
    }
    @PostMapping("/yee_update")
    public Result update(YeeManage yeeManage) throws Exception {
        return yeeManageService.update(yeeManage);
    }
    @GetMapping("/yee_lock")
    public Result lock(int schoolId, int id) throws Exception {
        return yeeManageService.lock(schoolId,id);
    }

    @GetMapping("/yee_active")
    public Result active(int schoolId, int id) throws Exception {
        return yeeManageService.active(schoolId,id);
    }
    @PostMapping("/yee_search")
    public Result search(YeeManageLike yeeManageLike) throws Exception {
        return yeeManageService.searchByCondition(yeeManageLike);
    }
    @PostMapping("yee_actives")
    public Result actives(ListID listID) throws Exception {
        return yeeManageService.actives(listID.getSchoolId(),listID.getId());
    }
    @PostMapping("yee_password")
    public Result password(ListID listID) throws Exception {
        return yeeManageService.password(listID.getSchoolId(),listID.getId());
    }
    @PostMapping("yee_add")
    public Result add(YeeManage yeeManage) throws Exception {
        return yeeManageService.add(yeeManage);
    }
}
