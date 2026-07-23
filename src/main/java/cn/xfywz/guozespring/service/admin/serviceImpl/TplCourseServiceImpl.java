package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlTplCourse;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.mapper.TplCourseMapper;
import cn.xfywz.guozespring.service.admin.TplCourseService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TplCourseServiceImpl implements TplCourseService {
    @Autowired
    private TplCourseMapper tplCourseMapper;


    @Override
    public Result selectAll(int PageSize, int PageNum) {
        Page<SlTplCourse> page = new Page<>(PageNum, PageSize);

        LambdaQueryWrapper<SlTplCourse> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        lambdaQueryWrapper
                .orderByDesc(SlTplCourse::getAddTime);

        page = tplCourseMapper.selectPage(page, lambdaQueryWrapper);

        return Result.success(page.getRecords(), page.getTotal());
    }

    @Override
    public Result add(SlTplCourse slTplCourse) {
        slTplCourse.setAddTime(new java.sql.Timestamp(System.currentTimeMillis()));
        return tplCourseMapper.insert(slTplCourse) > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    @Override
    public Result del(int id) {
        QueryWrapper<SlTplCourse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        if (tplCourseMapper.delete(queryWrapper)>0){
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }

    @Override
    public Result update(SlTplCourse slTplCourse) {
        tplCourseMapper.updateById(slTplCourse);
        if (tplCourseMapper.updateById(slTplCourse)>0){
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }


    @Override
    public Result search(TplCourseLike tplCourseLike, Integer pageNum, Integer pageSize) {
        // 1. 初始化分页参数，设置默认值避免空指针
        int currentPage = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int pageSizeNum = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        // 2. 创建分页对象
        Page<SlTplCourse> page = new Page<>(currentPage, pageSizeNum);

        // 3. 构建查询条件（优化了空值判断，更规范）
        QueryWrapper<SlTplCourse> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(tplCourseLike.getName())) {
            queryWrapper.like("name", tplCourseLike.getName());
        }
        if (StringUtils.hasText(tplCourseLike.getCode())) {
            queryWrapper.eq("code", tplCourseLike.getCode());
        }
        if (tplCourseLike.getCateBid() != null) {
            queryWrapper.eq("cateBid", tplCourseLike.getCateBid());
        }
        if (tplCourseLike.getCateMid() != null) {
            queryWrapper.eq("cateMid", tplCourseLike.getCateMid());
        }

        // 4. 执行分页查询
        IPage<SlTplCourse> pageResult = tplCourseMapper.selectPage(page, queryWrapper);

        // 5. 返回分页结果（包含数据列表、总条数、总页数等）
        return Result.success(pageResult.getRecords(), pageResult.getTotal());
    }
}
