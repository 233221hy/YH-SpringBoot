package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.service.teacher.YeeNodeFilesService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class StuYeeFileNodeController {
    @Autowired
    YeeNodeFilesService yeeNodeFilesService;
    @GetMapping("/node_files_select")
    public Result select(@RequestParam Integer schoolId,
                         @RequestParam long nodeId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeFilesService.selectByNodeId(schoolId, nodeId);
        } else {
            return Result.error("非法访问");
        }
    }
}
