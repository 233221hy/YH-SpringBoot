package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlTplDiscuss;
import cn.xfywz.guozespring.mapper.SlTplDiscussMapper;
import cn.xfywz.guozespring.service.admin.SlTplDiscussService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class SlTplDiscussServiceImpl implements SlTplDiscussService {
    @Autowired
    private SlTplDiscussMapper slTplDiscussMapper;
    @Override
    public Result showAll(int pageSize, int pageNum, int courseId) {
        QueryWrapper<SlTplDiscuss> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("courseId",courseId);
        Page<SlTplDiscuss> page = new Page<>(pageNum,pageSize);
        Page<SlTplDiscuss> page1 = slTplDiscussMapper.selectPage(page,queryWrapper);
        return Result.success(page1.getRecords(), page1.getTotal());
    }
    @Override
    public Result add(SlTplDiscuss slTplDiscuss) {
        slTplDiscuss.setAddTime(new Timestamp(System.currentTimeMillis()));
        int insert = slTplDiscussMapper.insert(slTplDiscuss);
        if (insert > 0) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }
    @Override
    public Result del(int id) {
        int delete = slTplDiscussMapper.deleteById(id);
        if (delete > 0) {
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }

    @Override
    public Result update(SlTplDiscuss slTplDiscuss) {
        int update = slTplDiscussMapper.updateById(slTplDiscuss);
        if (update > 0) {
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }
}
