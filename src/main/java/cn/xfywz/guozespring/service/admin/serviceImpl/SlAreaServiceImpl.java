package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlArea;
import cn.xfywz.guozespring.mapper.SlAreaMapper;
import cn.xfywz.guozespring.service.admin.SlAreaService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SlAreaServiceImpl implements SlAreaService {
    @Autowired
    private SlAreaMapper slAreaMapper;
    @Override
    public Result list() {
        QueryWrapper<SlArea> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid",0);
        List<SlArea> slA = slAreaMapper.selectList(queryWrapper);
        return Result.success(slA);
    }

    @Override
    public Result select(Integer pid) {
        QueryWrapper<SlArea> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid",pid);
        List<SlArea> slA = slAreaMapper.selectList(queryWrapper);
        return Result.success(slA);
    }
}
