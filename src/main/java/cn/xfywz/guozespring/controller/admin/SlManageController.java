package cn.xfywz.guozespring.controller.admin;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.service.admin.SlManageService;
import cn.xfywz.guozespring.service.admin.UserLoginService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/manage")
public class SlManageController {
    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private SlManageService slManageService;


    @PostMapping("/login")
    public Result login(SlManage slManage, HttpServletRequest request) {
        try {
            Map<String, Object> map = userLoginService.login(slManage,request);

            Object msgObj = map.get("msg");
            if (msgObj != null && msgObj instanceof String) {
                return Result.error("登录失败", (String) msgObj);
            }

            String token = (String) map.get("token");
            List<?> permissions = (List<?>) map.get("userPermissionList");
            return Result.successWithList(token, permissions);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("接口错误", e.getMessage());
        }
    }

    @GetMapping("/login_out")
    public Result loginOut(@RequestHeader String Authorization) throws Exception {
        userLoginService.loginOut(Authorization);
        return Result.success("退出成功");
    }
    //获取个人信息
    @GetMapping("/info")
    public Result Info(@RequestHeader String Authorization) throws Exception {
        if (Authorization != null){
            return Result.success(slManageService.info(Authorization));
        }
        return Result.error("请重新登录");
    }
    //更新个人信息
    @PostMapping("/info_update")
    public Result InfoUpdate(SlManage slManage,@RequestHeader String Authorization) throws Exception {
        if (Authorization != null){
            return Result.success(slManageService.infoUpdate(slManage,Authorization));
        }
        return Result.error("请重新登录");
    }
    @PostMapping("/info_update_password")
    public Result InfoUpdatePassword(String oldPassword,String newPassword,@RequestHeader String Authorization) throws Exception {
        if (Authorization != null){
            return Result.success(slManageService.infoUpdatePassword(oldPassword,newPassword,Authorization));
        }else return Result.error("请重新登录");
    }
    @GetMapping("/manage_list")
    public Result ManageList(@RequestParam int PageSize, @RequestParam int PageNum, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.selectAll(PageSize,PageNum);
        }else return Result.error("请重新登录");
    }
    @GetMapping("/delete")
    public Result Delete(@RequestParam int id, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.deleteById(id);
        }else return Result.error("请重新登录");
    }

    @PostMapping("/add")
    public Result add(SlManage slManage, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.addManage(slManage);
        }else return Result.error("请重新登录");
    }

    @PostMapping("/update")
    public Result update(SlManage slManage, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.updateManage(slManage);
        }else return Result.error("请重新登录");
    }

    @GetMapping("/select")
    public Result select(@RequestParam int id, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.selectById(id);
        }else return Result.error("请重新登录");
    }

    @GetMapping("/selectLikeName")
    public Result selectLikeName(@RequestParam String name, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.selectLikeName(name);
        }else return Result.error("请重新登录");
    }

    @GetMapping("/selectLikeAccount")
    public Result selectLikeAccount(@RequestParam String account, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.selectLikeAccount(account);
        }else return Result.error("请重新登录");
    }

    @GetMapping("/manage_is_lock")
    public Result manageIsLock(@RequestParam int id, @RequestHeader String Authorization) {
        if (Authorization != null){
            return slManageService.manageIsLock(id);
        }else return Result.error("请重新登录");
    }
}
