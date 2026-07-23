package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlTplNodeFiles;
import cn.xfywz.guozespring.mapper.SlTplNodeFilesMapper;
import cn.xfywz.guozespring.service.admin.SlTplNodeFilesService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlTplNodeFilesServiceImpl implements SlTplNodeFilesService {
    @Autowired
    private SlTplNodeFilesMapper slTplNodeFilesMapper;

    @Override
    public Result selectAll(int pageSize, int pageNum, int courseId) {
        Page<SlTplNodeFiles> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SlTplNodeFiles> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("courseId", courseId);
        slTplNodeFilesMapper.selectPage(page,queryWrapper);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @Override
    public Result add(SlTplNodeFiles slTplNodeFiles) {
        slTplNodeFiles.setAddTime(new java.sql.Timestamp(System.currentTimeMillis()));
        slTplNodeFiles.setTimeView(0);
        slTplNodeFiles.setFileName(slTplNodeFiles.getName());
        return slTplNodeFilesMapper.insert(slTplNodeFiles) > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }
    @Override
    public Result del(int id) {
        return slTplNodeFilesMapper.deleteById(id) > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result update(SlTplNodeFiles slTplNodeFiles) {
        QueryWrapper<SlTplNodeFiles> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", slTplNodeFiles.getId());
        if (slTplNodeFilesMapper.update(slTplNodeFiles,queryWrapper)>0){
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }
}
