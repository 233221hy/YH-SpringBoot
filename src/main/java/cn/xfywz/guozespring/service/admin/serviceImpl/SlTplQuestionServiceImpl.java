package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlTplQuestion;
import cn.xfywz.guozespring.entity.vo.SlTplQuestionLike;
import cn.xfywz.guozespring.mapper.SlTplQuestionMapper;
import cn.xfywz.guozespring.service.admin.SlTplQuestionService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlTplQuestionServiceImpl implements SlTplQuestionService {
    @Autowired
    private SlTplQuestionMapper slTplQuestionMapper;

    @Override
    public Result selectAll(int PageSize, int PageNum) {
        Page<SlTplQuestion> page = new Page<>(PageNum, PageSize);
        Page<SlTplQuestion> page1 = slTplQuestionMapper.selectPage(page, null);
        return Result.success(page1.getRecords(), page1.getTotal());
    }

    @Override
    public Result add(SlTplQuestion slTplQuestion) {
        slTplQuestion.setAddTime(new java.sql.Date(System.currentTimeMillis()));
        return slTplQuestionMapper.insert(slTplQuestion) > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    @Override
    public Result del(int id) {
        return slTplQuestionMapper.deleteById(id) > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result update(SlTplQuestion slTplQuestion) {
        return slTplQuestionMapper.updateById(slTplQuestion) > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    @Override
    public Result search(SlTplQuestionLike slTplQuestionLike) {
        Page<SlTplQuestion> page = new Page<>(slTplQuestionLike.getPageNum(), slTplQuestionLike.getPageSize());
        QueryWrapper<SlTplQuestion> queryWrapper = new QueryWrapper<>();
        if (slTplQuestionLike.getLike() != null)
            queryWrapper.like("title",slTplQuestionLike.getLike())
            .or()
            .like("topic", slTplQuestionLike.getLike());
        if (slTplQuestionLike.getType() != null)
            queryWrapper.eq("type", slTplQuestionLike.getType());
        if (slTplQuestionLike.getLevel() != null)
            queryWrapper.eq("level", slTplQuestionLike.getLevel());
        if (slTplQuestionLike.getCateBid() != null)
            queryWrapper.eq("cateBid", slTplQuestionLike.getCateBid());
        if (slTplQuestionLike.getCateMid() != null && slTplQuestionLike.getCateMid() != 0)
            queryWrapper.eq("cateMid", slTplQuestionLike.getCateMid()).eq("cateBid", slTplQuestionLike.getCateBid());
        Page<SlTplQuestion> page1 = slTplQuestionMapper.selectPage(page, queryWrapper);
        return Result.success(page1.getRecords(), page1.getTotal());
    }
}
