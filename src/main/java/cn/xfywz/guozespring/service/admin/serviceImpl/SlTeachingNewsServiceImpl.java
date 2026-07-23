package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlTeachingNews;
import cn.xfywz.guozespring.mapper.SlTeachingNewsMapper;
import cn.xfywz.guozespring.service.admin.SlTeachingNewsService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SlTeachingNewsServiceImpl implements SlTeachingNewsService {
    @Autowired
    private SlTeachingNewsMapper slTeachingNewsMapper;

    @Override
    public Result add(SlTeachingNews slTeachingNews) {
        slTeachingNews.setAddTime(new java.sql.Timestamp(System.currentTimeMillis()));
        if (slTeachingNewsMapper.insert(slTeachingNews)>0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result delete(Integer id) {
        if (slTeachingNewsMapper.deleteById(id)>0){
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }

    @Override
    public Result update(SlTeachingNews slTeachingNews) {
        if (slTeachingNewsMapper.updateById(slTeachingNews)>0){
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }

    @Override
    public Result list(Integer pageNum, Integer pageSize) {
        Page<SlTeachingNews> page = new Page<>(pageNum, pageSize);
        Page<SlTeachingNews> page1 = slTeachingNewsMapper.selectPage(page, null);
        return Result.success(page1.getRecords(), Long.valueOf(page1.getTotal()));
    }

    @Override
    public Result selectLike(String title) {
        QueryWrapper<SlTeachingNews> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("title",title);
        List<SlTeachingNews> list=slTeachingNewsMapper.selectList(queryWrapper);
        return Result.success((Object) list, (long) list.size());
    }

    @Override
    public Result listAll(Integer pageNum, Integer pageSize) {
        QueryWrapper<SlTeachingNews> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow", 1);
        Page<SlTeachingNews> page = new Page<>(pageNum, pageSize);
        Page<SlTeachingNews> page1 = slTeachingNewsMapper.selectPage(page, queryWrapper);
        return Result.success(page1.getRecords(), page1.getTotal());
    }
}



