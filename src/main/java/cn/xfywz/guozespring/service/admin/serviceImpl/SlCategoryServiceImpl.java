package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlCategory;
import cn.xfywz.guozespring.mapper.SlCategoryMapper;
import cn.xfywz.guozespring.service.admin.SlCategoryService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlCategoryServiceImpl implements SlCategoryService {
    @Autowired
    private SlCategoryMapper slCategoryMapper;
    @Override
    public Result add(SlCategory slCategory) {
        if (slCategoryMapper.insert(slCategory)==1){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlCategory slCategory) {
        if (slCategoryMapper.updateById(slCategory)==1){
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }

    @Override
    public Result delete(Integer id) {
        if (slCategoryMapper.deleteById(id)>0){
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }
}
