package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlOpenCourseFiles;
import cn.xfywz.guozespring.mapper.SlOpenCourseFilesMapper;
import cn.xfywz.guozespring.service.admin.SlOpenCourseFilesService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class SlOpenCourseFilesServiceImpl implements SlOpenCourseFilesService {
    @Autowired
    private SlOpenCourseFilesMapper slOpenCourseFilesMapper;
    @Override
    public Result List(Integer id) {
        QueryWrapper<SlOpenCourseFiles> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("courseId",id);
        List<SlOpenCourseFiles> slOpenCourseFiles= slOpenCourseFilesMapper.selectList(queryWrapper);
        return Result.success((Object) slOpenCourseFiles, (long) slOpenCourseFiles.size());
    }

    @Override
    public Result add(SlOpenCourseFiles slOpenCourseFiles) {
        slOpenCourseFiles.setAddTime(new Timestamp(System.currentTimeMillis()));
        int insert = slOpenCourseFilesMapper.insert(slOpenCourseFiles);
        if (insert > 0) {
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlOpenCourseFiles slOpenCourseFiles) {
        int update = slOpenCourseFilesMapper.updateById(slOpenCourseFiles);
        if (update > 0) {
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }

    @Override
    public Result del(Integer id) {
        int delete = slOpenCourseFilesMapper.deleteById(id);
        if (delete > 0) {
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }
}
