package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeNodeFiles;
import cn.xfywz.guozespring.service.teacher.YeeNodeFilesService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
public class YeeNodeFilesController {
    @Autowired
    private YeeNodeFilesService yeeNodeFilesService;

    @GetMapping("/yee_node_files_select")
    public Result select(@RequestParam Integer schoolId,
                         @RequestParam long nodeId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeFilesService.selectByNodeId(schoolId, nodeId);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_node_files_add")
    public Result add(@RequestHeader String Authorization, YeeNodeFiles yeeNodeFiles) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeNodeFiles.getSchoolId())) {
            return yeeNodeFilesService.add(yeeNodeFiles);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_node_files_update")
    public Result update(@RequestHeader String Authorization, YeeNodeFiles yeeNodeFiles) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeNodeFiles.getSchoolId())) {
            return yeeNodeFilesService.update(yeeNodeFiles);
        } else {
            return Result.error("非法访问");
        }
    }

    @GetMapping("/yee_node_files_delete")
    public Result delete(@RequestParam Integer schoolId,
                         @RequestParam long id,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeFilesService.delete(schoolId, id);
        } else {
            return Result.error("非法访问");
        }
    }
}