package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlDocument;
import cn.xfywz.guozespring.mapper.SlDocumentMapper;
import cn.xfywz.guozespring.service.admin.SlDocumentService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class SlDocumentServiceImpl implements SlDocumentService {
    @Autowired
    private SlDocumentMapper slDocumentMapper;
    @Override
    public Result add(SlDocument slDocument) {
        slDocument.setAddTime(new Timestamp(System.currentTimeMillis()));
        int insert = slDocumentMapper.insert(slDocument);
        return insert > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    @Override
    public Result delete(Integer id) {
        int delete = slDocumentMapper.deleteById(id);
        return delete > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result update(SlDocument slDocument) {
        int update = slDocumentMapper.updateById(slDocument);
        return update > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

    @Override
    public Result list(Integer pageNum, Integer pageSize) {
        Page<SlDocument> page = new Page<>(pageNum, pageSize);
        Page<SlDocument> page1 = slDocumentMapper.selectPage(page, null);
        return Result.success(page1.getRecords(), page1.getTotal());
    }

    @Override
    public Result selectLike(String name) {
        QueryWrapper<SlDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("title",name);
        return Result.success(slDocumentMapper.selectList(queryWrapper));
    }
}
