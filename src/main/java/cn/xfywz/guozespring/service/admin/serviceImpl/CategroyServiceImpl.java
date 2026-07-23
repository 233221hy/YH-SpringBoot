package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.Categroy;
import cn.xfywz.guozespring.entity.mhmain.SlCategory;
import cn.xfywz.guozespring.mapper.SlCategoryMapper;
import cn.xfywz.guozespring.service.admin.CategroyService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategroyServiceImpl implements CategroyService {
    @Autowired
    private SlCategoryMapper slCategoryMapper;

    @Override
    public Result selectAll() {
        List<Categroy> categroys=new ArrayList<>();
        List<SlCategory> list = slCategoryMapper.selectAll();
        for (SlCategory slCategory : list) {
            Categroy categroy=new Categroy();
            categroy.setName(slCategory.getName());
            categroy.setId(slCategory.getId());
            List<SlCategory> slCategory1=slCategoryMapper.selectById(slCategory.getId());
            categroy.setSlCategory(slCategory1);
            categroys.add(categroy);
        }
        return Result.success(categroys);
    }

    @Override
    public Result categroyList() {
        return Result.success(slCategoryMapper.selectList(null));
    }
}
