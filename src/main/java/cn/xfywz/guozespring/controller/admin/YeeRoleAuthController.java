package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.entity.mhsch.YeeRoleAuth;
import cn.xfywz.guozespring.entity.vo.YeeRoleAuthBatch;
import cn.xfywz.guozespring.service.admin.SlRoleService;
import cn.xfywz.guozespring.service.admin.YeeRoleAuthService;
import cn.xfywz.guozespring.service.admin.serviceImpl.YeeRoleAuthServiceImpl;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
public class YeeRoleAuthController {

    @Autowired
    private YeeRoleAuthService yeeRoleAuthService;
    @Autowired
    private SlRoleService slRoleService;

    @GetMapping("/yee_role_auth_list")
    public Result roleAuth_list(@RequestParam long schoolId, @RequestParam int PageSize, @RequestParam int PageNum) {
        try {
            return yeeRoleAuthService.roleAuth_list(schoolId, PageSize, PageNum);
        } catch (Exception e) {
            return Result.error("查询权限管理列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/yee_role_auth_update")
    public Result roleAuth_update(@RequestBody YeeRoleAuth yeeRoleAuth) {
        try {
            return yeeRoleAuthService.roleAuth_update(yeeRoleAuth);
        } catch (Exception e) {
            return Result.error("更新权限管理失败: " + e.getMessage());
        }
    }

    @GetMapping("/yee_role_auth_delete")
    public Result roleAuth_delete(@RequestParam long id, @RequestParam long schoolId) {
        try {
            return ((YeeRoleAuthServiceImpl) yeeRoleAuthService).roleAuth_delete(id, schoolId);
        } catch (Exception e) {
            return Result.error("删除权限管理失败: " + e.getMessage());
        }
    }

    @GetMapping("/yee_role_auth_delete_by_role")
    public Result roleAuth_delete_by_role(@RequestParam long roleId, @RequestParam long schoolId) {
        try {
            return yeeRoleAuthService.roleAuth_delete_by_role(roleId, schoolId);
        } catch (Exception e) {
            return Result.error("删除角色权限失败: " + e.getMessage());
        }
    }

    @GetMapping("/yee_role_auth_search_by_role")
    public Result roleAuth_search_by_role(@RequestParam long roleId, @RequestParam long schoolId) {
        try {
            return yeeRoleAuthService.roleAuth_search_by_role(roleId, schoolId);
        } catch (Exception e) {
            return Result.error("查询角色权限失败: " + e.getMessage());
        }
    }

    @GetMapping("/yee_role_auth_search_by_auth")
    public Result roleAuth_search_by_auth(@RequestParam long authId, @RequestParam long schoolId) {
        try {
            return yeeRoleAuthService.roleAuth_search_by_auth(authId, schoolId);
        } catch (Exception e) {
            return Result.error("查询权限角色失败: " + e.getMessage());
        }
    }

    @PostMapping("/role_add")
    public Result add(SlRole slRole , @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization,(int)slRole.getSchoolId()))
            return slRoleService.add(slRole);
        else {
            return Result.error("非法请求");
        }
    }

    @PostMapping("/role_update")
    public Result update(SlRole slRole , @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization,(int)slRole.getSchoolId()))
            return slRoleService.update(slRole);
        else {
            return Result.error("非法请求");
        }
    }

    @PostMapping("/yee_role_auth_batch_add")
    public Result roleAuth_batch_add(@RequestBody YeeRoleAuthBatch yeeRoleAuthBatch) {
        try {
            return yeeRoleAuthService.roleAuth_batch_add(
                    yeeRoleAuthBatch.getRoleId(),
                    yeeRoleAuthBatch.getSchoolId(),
                    yeeRoleAuthBatch.getAuthIds()
            );
        } catch (Exception e) {
            return Result.error("批量设置权限失败: " + e.getMessage());
        }
    }
}