package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlOpenAnnouncement;
import cn.xfywz.guozespring.mapper.SlOpenAnnouncementMapper;
import cn.xfywz.guozespring.service.admin.SlOpenAnnouncementService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;

@Service
public class SlOpenAnnouncementServiceImpl implements SlOpenAnnouncementService {
    @Autowired
    private SlOpenAnnouncementMapper slOpenAnnouncementMapper;
    @Override
    public Result select(Integer id) {
        QueryWrapper<SlOpenAnnouncement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("courseId",id);
        return Result.success(slOpenAnnouncementMapper.selectList(queryWrapper));
    }

    @Override
    public Result add(SlOpenAnnouncement slOpenAnnouncement) {
        slOpenAnnouncement.setAddTime(new Timestamp(System.currentTimeMillis()));
        int insert = slOpenAnnouncementMapper.insert(slOpenAnnouncement);
        if (insert > 0) {
            return Result.success("添加成功");
        }else return Result.error("添加失败");
    }

    @Override
    public Result update(SlOpenAnnouncement slOpenAnnouncement) {
        int update = slOpenAnnouncementMapper.updateById(slOpenAnnouncement);
        return update > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

    @Override
    public Result delete(Integer id) {
        int delete = slOpenAnnouncementMapper.deleteById(id);
        return delete > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}
