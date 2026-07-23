package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeMuted;
import cn.xfywz.guozespring.entity.dto.YeeMutedQueryParam;
import cn.xfywz.guozespring.service.teacher.YeeMutedService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeMutedController {

    @Autowired
    private YeeMutedService mutedService;

    //查询禁言列表
    @RequestMapping("/yee_muted_list")
    public Result list(@RequestParam int pageSize,
                       @RequestParam int pageNum,
                       @RequestParam int schoolId) throws Exception {
        return mutedService.list(pageSize, pageNum, schoolId);
    }

    //增加禁言信息
    @PostMapping("/yee_muted_add")
    public Result add(@RequestBody YeeMuted muted) throws Exception {
        mutedService.add(muted);
        return Result.success();
    }

    //删除禁言信息
    @GetMapping("/yee_muted_delete")
    public Result delete(@RequestParam Integer id,
                         @RequestParam int schoolId) throws Exception {
        mutedService.delete(id,schoolId);
        return Result.success();
    }

    //条件查询禁言信息
    @PostMapping("/yee_muted_search")
    public Result search(@RequestBody YeeMutedQueryParam param) throws Exception {
        return mutedService.searchByCondition(param);
    }
}
