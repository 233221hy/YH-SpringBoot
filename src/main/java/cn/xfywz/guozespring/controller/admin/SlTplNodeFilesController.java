package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplNodeFiles;
import cn.xfywz.guozespring.service.admin.SlTplNodeFilesService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlTplNodeFilesController {
    @Autowired
    SlTplNodeFilesService slTplNodeFilesService;

    @GetMapping("/tpl_node_files_list")
    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum,@RequestParam int courseId) {
        return slTplNodeFilesService.selectAll(PageSize, PageNum,courseId);
    }
    @PostMapping("/tpl_node_files_add")
    public Result add(SlTplNodeFiles slTplNodeFiles) {
        return slTplNodeFilesService.add(slTplNodeFiles);
    }
    @GetMapping("/del_tpl_node_files")
    public Result del(@RequestParam int id) {
        return slTplNodeFilesService.del(id);
    }
    @PostMapping("/tpl_node_files_update")
    public Result update(SlTplNodeFiles slTplNodeFiles) {
        return slTplNodeFilesService.update(slTplNodeFiles);
    }
}
