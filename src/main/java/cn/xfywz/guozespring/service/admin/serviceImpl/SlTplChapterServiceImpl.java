package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlTplChapter;
import cn.xfywz.guozespring.mapper.SlTplChapterMapper;
import cn.xfywz.guozespring.service.admin.SlTplChapterService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlTplChapterServiceImpl implements SlTplChapterService {
    @Autowired
    private SlTplChapterMapper slTplChapterMapper;
    @Override
    public Result add(SlTplChapter slTplChapter) {
        if (slTplChapterMapper.insert(slTplChapter) > 0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlTplChapter slTplChapter) {
        return slTplChapterMapper.updateById(slTplChapter) > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

    @Override
    public Result del(int id) {
        return slTplChapterMapper.deleteById(id) > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}
