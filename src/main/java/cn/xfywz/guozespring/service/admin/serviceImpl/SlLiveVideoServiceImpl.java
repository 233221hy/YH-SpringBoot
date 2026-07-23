package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlLiveVideo;
import cn.xfywz.guozespring.mapper.SlLiveVideoMapper;
import cn.xfywz.guozespring.service.admin.SlLiveVideoService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlLiveVideoServiceImpl implements SlLiveVideoService {
    @Autowired
    private SlLiveVideoMapper slLiveVideoMapper;
    @Override
    public Result add(SlLiveVideo slLiveVideo) {
        int insert = slLiveVideoMapper.insert(slLiveVideo);
        if (insert > 0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result delete(Integer id) {
        int delete = slLiveVideoMapper.deleteById(id);
        return delete > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result update(SlLiveVideo slLiveVideo) {
        int update = slLiveVideoMapper.updateById(slLiveVideo);
        return update > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

    @Override
    public Result list(Integer pageNum, Integer pageSize) {
        Page<SlLiveVideo> page = new Page<>(pageNum, pageSize);
        Page<SlLiveVideo> page1 = slLiveVideoMapper.selectPage(page, null);
        return Result.success(page1.getRecords(),page1.getTotal());
    }

    @Override
    public Result selectLike(String name) {
        QueryWrapper<SlLiveVideo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name",name);
        return Result.success(slLiveVideoMapper.selectList(queryWrapper));
    }

    @Override
    public Result listAll(int pageSize, int pageNum) {
        QueryWrapper<SlLiveVideo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow",1);
        Page<SlLiveVideo> page = new Page<>(pageNum, pageSize);
        Page<SlLiveVideo> page1 = slLiveVideoMapper.selectPage(page, queryWrapper);
        return Result.success(page1.getRecords(),page1.getTotal());
    }
}
