package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.dto.YeeManageQueryParam;
import cn.xfywz.guozespring.service.teacher.TeacherYeeManageService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.EncodePasswordUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; 
import java.util.List; 
import cn.xfywz.guozespring.util.CosClientUtil; // 新增


@RestController
@RequestMapping("/school")
public class TeacherYeeManageController {

    @Qualifier("teacherYeeManageService")
    @Autowired
    private TeacherYeeManageService teacherYeeManageService;

    //获取个人信息
    @GetMapping("/yee_manage_info")
    public Result getInfo(@RequestHeader String Authorization) throws Exception{
        if (Authorization != null){
            return Result.success(teacherYeeManageService.getInfo(Authorization));
        }
        return Result.error("获取失败");
    }

    //修改个人信息
    @PostMapping("/yee_manage_info_update")
    public Result InfoUpdate(@RequestHeader String Authorization, @RequestBody YeeManage yeeManage) throws Exception {
        if(Authorization != null){
            return Result.success(teacherYeeManageService.infoUpdate(Authorization,yeeManage));
        }
        return Result.error("非法访问");
    }

    //修改密码
    @Validated
    @PostMapping("/yee_manage_info_update_pwd")
    public Result InfoUpdatePassword(@RequestHeader String Authorization,
                                     @NotNull String oldPassword,
                                     @NotNull String newPassword) throws Exception {
        // 密码校验
        EncodePasswordUtil.validatePasswordStrength(newPassword);
        if (Authorization != null){
            return Result.success(teacherYeeManageService.infoUpdatePassword(oldPassword,newPassword,Authorization));
        }
        return Result.error("非法访问");
    }

    //查询账户列表
    @GetMapping("/yee_manage_list")
    public Result selectAll(YeeManageQueryParam param,@RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization,param.getSchoolId())){
            return teacherYeeManageService.selectAll( param);
        }else return Result.error("非法访问");
    }

    //新增账户（支持上传头像文件）
    @PostMapping("/yee_manage_add")
    public Result add(@RequestHeader String Authorization,
                      YeeManage yeeManage,
                      @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, yeeManage.getSchoolId())) {
            // 若前端以 multipart/form-data 同时上传头像文件，则先上传至 COS，拿到 URL 后写入 avatar 字段
            if (file != null && !file.isEmpty()) {
                String url = CosClientUtil.upload(file);
                if (url == null || !url.startsWith("http")) {
                    return Result.error("头像上传失败");
                }
                yeeManage.setAvatar(url);
            }
            return Result.success(teacherYeeManageService.add(yeeManage));
        }else return Result.error("非法访问");
    }

    //修改账户信息
    @PostMapping("/yee_manage_update")
    public Result update(@RequestHeader String Authorization, @RequestBody YeeManage yeeManage) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeManage.getSchoolId())) {
            teacherYeeManageService.update(yeeManage);
            return Result.success();
        }else return Result.error("非法访问");
    }

    //删除账户
    @GetMapping("/yee_manage_delete")
    public Result delete(@RequestParam Long id, @RequestParam String account ,@RequestHeader String Authorization, int schoolId) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            teacherYeeManageService.delete(id, schoolId, account);
            return Result.success();
        }else return Result.error("非法访问");
    }

    //账户锁定
    @GetMapping("/yee_manage_lock")
    public Result lock(@RequestParam Long id, @RequestHeader String Authorization, int schoolId) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            teacherYeeManageService.lock(id, schoolId);
            return Result.success();
        }else return Result.error("非法访问");
    }

    //条件模糊查询
    @PostMapping("/yee_manage_search")
    public Result searchByCondition(@RequestBody YeeManageQueryParam param, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, Math.toIntExact(param.getSchoolId()))) {
            return teacherYeeManageService.searchByCondition(param);
        }else return Result.error("非法访问");
    }

    //根据id查用户信息
    @GetMapping("/yee_manage_searchById")
    public Result searchById(@RequestParam Long id,
                             @RequestParam(value = "schoolId", defaultValue = "0") Integer schoolId,
                             @RequestHeader String Authorization) throws Exception {
        if (schoolId < 0) {
            return Result.error("schoolId 不能为空或无效");
        }
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.success(teacherYeeManageService.searchById(id, schoolId));
        }else return Result.error("非法访问");
    }

    // 批量导入教师
    @PostMapping("/yee_manage_import")
    public Result importData(@RequestHeader String Authorization,
                            @RequestParam int schoolId,
                            @RequestParam(required = false) Long collegeId,
                            @RequestPart("file") MultipartFile file) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return teacherYeeManageService.importData(schoolId, collegeId, file);
        } else return Result.error("非法访问");
    }

    // 批量重置教师密码

    @PostMapping("/yee_manage_pwd_reset")
    public Result passwordReset(@RequestHeader String Authorization,
                                @RequestParam int schoolId,
                                @RequestParam List<Long> id) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return teacherYeeManageService.passwordReset(schoolId, id);
        } else return Result.error("非法访问");
    }


}
