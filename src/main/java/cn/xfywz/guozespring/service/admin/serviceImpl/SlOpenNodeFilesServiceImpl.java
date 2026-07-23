package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNodeFiles;
import cn.xfywz.guozespring.mapper.SlOpenNodeFilesMapper;
import cn.xfywz.guozespring.service.admin.SlOpenNodeFilesService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SlOpenNodeFilesServiceImpl implements SlOpenNodeFilesService {
    @Autowired
    private SlOpenNodeFilesMapper slOpenNodeFilesMapper;
    @Override
    public Result add(SlOpenNodeFiles slOpenNodeFiles) {
        slOpenNodeFiles.setAddTime(new java.sql.Timestamp(System.currentTimeMillis()));
        if (slOpenNodeFilesMapper.insert(slOpenNodeFiles)>0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlOpenNodeFiles slOpenNodeFiles) {
        if (slOpenNodeFilesMapper.updateById(slOpenNodeFiles)>0){
            return Result.success("更新成功");
        }else return Result.error("更新失败");
    }

    @Override
    public Result delete(Integer id) {
        QueryWrapper<SlOpenNodeFiles> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        if (slOpenNodeFilesMapper.delete(queryWrapper)>0){
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }

    @Override
    public Result selectById(Integer id) {
        QueryWrapper<SlOpenNodeFiles> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("nodeId",id);
        List<SlOpenNodeFiles> slOpenNodeFiles = slOpenNodeFilesMapper.selectList(queryWrapper);
        return Result.success(slOpenNodeFiles);
    }
}
