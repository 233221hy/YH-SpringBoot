package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlOpenNode;
import cn.xfywz.guozespring.mapper.SlOpenNodeMapper;
import cn.xfywz.guozespring.service.admin.SlOpenNodeService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlOpenNodeServiceImpl implements SlOpenNodeService {
    @Autowired
    private SlOpenNodeMapper slOpenNodeMapper;
    @Override
    public Result add(SlOpenNode slOpenNode) {
       if (slOpenNodeMapper.insert(slOpenNode)>0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlOpenNode slOpenNode) {
        if (slOpenNodeMapper.updateById(slOpenNode)>0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result delete(Integer id) {
        QueryWrapper<SlOpenNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        if (slOpenNodeMapper.delete(queryWrapper)>0){
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }
}
