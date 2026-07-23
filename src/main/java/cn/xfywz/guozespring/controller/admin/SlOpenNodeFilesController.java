package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNodeFiles;
import cn.xfywz.guozespring.service.admin.SlOpenNodeFilesService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage")
public class SlOpenNodeFilesController {
    @Autowired
    private SlOpenNodeFilesService slOpenNodeFilesService;
    @PostMapping("/open_node_files_add")
    public Result add(SlOpenNodeFiles slOpenNodeFiles) {
            return slOpenNodeFilesService.add(slOpenNodeFiles);
    }
    @PostMapping ("/open_node_files_update")
    public Result update(SlOpenNodeFiles slOpenNodeFiles) {
            return slOpenNodeFilesService.update(slOpenNodeFiles);
    }
    @GetMapping("/open_node_files_del")
    public Result delete(Integer id) {
            return slOpenNodeFilesService.delete(id);
    }
    @GetMapping("/open_node_files_list")
    public Result selectById(Integer id) {
            return slOpenNodeFilesService.selectById(id);
    }

}
