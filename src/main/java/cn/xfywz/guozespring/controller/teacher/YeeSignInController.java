package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeSignIn;
import cn.xfywz.guozespring.service.teacher.YeeSignInService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeSignInController {
    @Autowired
    private YeeSignInService yeeSignInService;


    @GetMapping("/yee_sign_in_list")
    public Result listSignIn(@RequestParam Integer schoolId,
                             @RequestParam Integer courseId,
                             @RequestParam Integer pageSize,
                             @RequestParam Integer pageNum) throws Exception {
        return yeeSignInService.listSignIn(schoolId, courseId, pageSize, pageNum);
    }

    @PostMapping("/yee_sign_in_add")
    public Result addSignIn(YeeSignIn yeeSignIn) throws Exception {
        return yeeSignInService.addSignIn(yeeSignIn);
    }

    @GetMapping("/yee_sign_in_del")
    public Result delSignIn(@RequestParam Integer id,
                            @RequestParam Integer schoolId) throws Exception {
        return yeeSignInService.delSignIn(id, schoolId);
    }

    @GetMapping("/yee_sign_in_like")
    public Result likeSignIn(@RequestParam Integer schoolId,
                             @RequestParam Integer courseId,
                             @RequestParam String name) throws Exception {
        return yeeSignInService.likeSignIn(schoolId, courseId, name);
    }
    @PostMapping("/yee_sign_in_update")
    public Result updateSignIn(YeeSignIn yeeSignIn) throws Exception {
        return yeeSignInService.updateSignIn(yeeSignIn);
    }


}