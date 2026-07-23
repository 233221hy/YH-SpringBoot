package cn.xfywz.guozespring.service.admin.serviceImpl;


import cn.xfywz.guozespring.entity.mhmain.SlOpinion;
import cn.xfywz.guozespring.entity.vo.SlOpinionLike;
import cn.xfywz.guozespring.mapper.SlOpinionMapper;
import cn.xfywz.guozespring.service.admin.SlOpinionService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class SlOpinionServiceImpl implements SlOpinionService {
    @Autowired
    private SlOpinionMapper slOpinionMapper;
    @Override
    public Result add(SlOpinion slOpinion) {
        int insert = slOpinionMapper.insert(slOpinion);
        slOpinion.setAddTime(new Timestamp(System.currentTimeMillis()));
        return insert > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    @Override
    public Result delete(Integer id) {
        int delete = slOpinionMapper.deleteById(id);
        return delete > 0?Result.success("删除成功"):Result.error("删除失败");
    }

    @Override
    public Result list(Integer pageNum, Integer pageSize) {
        Page<SlOpinion> page = new Page<>(pageNum, pageSize);
        Page<SlOpinion> page1 = slOpinionMapper.selectPage(page, null);
        return Result.success(page1.getRecords(), page1.getTotal());
    }

    @Override
    public Result like(SlOpinionLike like) {
        Page<SlOpinion> page = new Page<>(like.getPageNum(), like.getPageSize());
        QueryWrapper<SlOpinion> queryWrapper = new QueryWrapper<>();
        if (like.getType()!= null){
            queryWrapper.eq("type",like.getType());
        }
        // 添加时间范围查询条件
        if (like.getStartTime() != null && like.getEndTime() != null) {
            queryWrapper.ge("addDate", like.getStartTime()).and(w -> w.le("addDate", like.getEndTime()));
        }
        Page<SlOpinion> page1 = slOpinionMapper.selectPage(page, queryWrapper);
        // 执行查询
        return Result.success(page1.getRecords(), page1.getTotal());
    }

}
