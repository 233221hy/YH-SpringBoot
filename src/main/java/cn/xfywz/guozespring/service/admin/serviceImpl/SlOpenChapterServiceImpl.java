package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlOpenChapter;
import cn.xfywz.guozespring.mapper.SlOpenChapterMapper;
import cn.xfywz.guozespring.service.admin.SlOpenChapterService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SlOpenChapterServiceImpl implements SlOpenChapterService {
    @Autowired
    private SlOpenChapterMapper slOpenChapterMapper;
    public Result selectId(int id){
        QueryWrapper<SlOpenChapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("courseId", Optional.of(id));
        List<SlOpenChapter> slOpenChapters = slOpenChapterMapper.selectList(queryWrapper);
        return Result.success(slOpenChapters);
    }
    public Result add(SlOpenChapter slOpenChapter){
        if (slOpenChapterMapper.insert(slOpenChapter)>0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }
    public Result del(int id){
        if (slOpenChapterMapper.deleteById((Integer)id)>0){
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }
    public Result update(SlOpenChapter slOpenChapter){
        if (slOpenChapterMapper.updateById(slOpenChapter)>0){
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }
}
