package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.YeeSignInRecordQuery;
import cn.xfywz.guozespring.entity.mhsch.YeeSignInRecord;
import cn.xfywz.guozespring.service.student.YeeSignInRecordService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RequireAuth
@RestController("teacherYeeSignInRecordController")
@RequestMapping("/school")
public class YeeSignInRecordController {

    @Autowired
    private YeeSignInRecordService yeeSignInRecordService;

    @PostMapping("/sign_in_record_list")
    public Result list(@RequestBody YeeSignInRecordQuery param) throws Exception {
        return yeeSignInRecordService.list(param);
    }

    @PostMapping("/sign_in_record_retroactive_signing")
    public Result update(@RequestBody YeeSignInRecord param) throws Exception {
        yeeSignInRecordService.update(param);
        return Result.success();
    }

    @PostMapping("/sign_in_record_export")
    public void exportData(@RequestBody YeeSignInRecordQuery param, HttpServletResponse response) throws Exception {
        yeeSignInRecordService.exportData(param, response);
    }
}
