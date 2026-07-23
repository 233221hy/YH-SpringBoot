package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlTplNode;
import cn.xfywz.guozespring.mapper.SlTplNodeMapper;
import cn.xfywz.guozespring.service.admin.SlTplNodeService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SlTplNodeServiceImpl implements SlTplNodeService {
    @Autowired
    private SlTplNodeMapper slTplNodeMapper;
    @Override
    public Result add(SlTplNode slTplNode) {
        if (slTplNodeMapper.insert(slTplNode)>0) {
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlTplNode slTplNode) {
        if (slTplNodeMapper.updateById(slTplNode)>0){
            return Result.success("修改成功");
        }else return Result.error("修改失败");
    }

    @Override
    public Result del(int id) {
        if (slTplNodeMapper.deleteById(id)>0){
            return Result.success("删除成功");
        }else return Result.error("删除失败");
    }
}
