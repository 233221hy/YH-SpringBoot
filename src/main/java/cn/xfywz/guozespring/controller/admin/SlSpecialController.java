package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.dto.SlSchoolThematicWithCourseNames;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import cn.xfywz.guozespring.entity.mhmain.SlSchoolThematic;
import cn.xfywz.guozespring.service.admin.SlOpenCourseService;
import cn.xfywz.guozespring.service.admin.SlSchoolThematicService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlSpecialController {

    @Autowired
    private SlSchoolThematicService slSchoolThematicService;


    @PostMapping("/sl_school_thematic_add")
    public Result add(@RequestBody SlSchoolThematic slSchoolThematic) {
        boolean success = slSchoolThematicService.save(slSchoolThematic);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    @GetMapping("/sl_school_thematic_delete")
    public Result delete(@RequestParam Integer id) {
        boolean success = slSchoolThematicService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    @PostMapping("/sl_school_thematic_update")
    public Result update(@RequestBody SlSchoolThematic slSchoolThematic) {
        boolean success = slSchoolThematicService.updateById(slSchoolThematic);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    @GetMapping("/sl_school_thematic_get")
    public Result get(@RequestParam Integer id) {
        SlSchoolThematic thematic = slSchoolThematicService.getById(id);
        if (thematic == null) {
            return Result.error("专题不存在");
        }
        return slSchoolThematicService.selectById(thematic);
    }



    @GetMapping("/sl_school_thematic_list")
    public Result list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer allow
    ) {
        Page<SlSchoolThematic> page = new Page<>(pageNum, pageSize);

        QueryWrapper<SlSchoolThematic> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like("name", name);
        }
        if (allow != null) {
            wrapper.eq("allow", allow);
        }
        if (schoolId != null) {
            wrapper.eq("schoolId", schoolId);
        }

        slSchoolThematicService.page(page, wrapper);

        return Result.success(page);
    }

    /**
     * 禁用 / 启用
     */
    @GetMapping("/sl_school_thematic_toggle")
    public Result toggleAllow(
            @RequestParam Integer id,
            @RequestParam Integer allow
    ) {
        // 参数校验
        if (allow == null || (allow != 0 && allow != 1)) {
            return Result.error("allow 参数必须为 0（禁用）或 1（启用）");
        }

        SlSchoolThematic thematic = slSchoolThematicService.getById(id);
        if (thematic == null) {
            return Result.error("专题不存在");
        }

        thematic.setAllow(allow);
        boolean success = slSchoolThematicService.updateById(thematic);

        String msg = (allow == 1) ? "启用成功" : "禁用成功";
        return success ? Result.success(msg) : Result.error("操作失败");
    }

    /**
     * 按域名查询学校专题课程信息
     */

}
