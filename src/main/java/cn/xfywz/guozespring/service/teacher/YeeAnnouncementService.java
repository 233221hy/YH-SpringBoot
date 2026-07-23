package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeAnnouncement;
import cn.xfywz.guozespring.util.Result;

public interface YeeAnnouncementService {
    Result selectAll(Integer schoolId, long courseId, int pageNum, int pageSize);
    Result add(YeeAnnouncement announcement);
    Result update(YeeAnnouncement announcement);
    Result delete(Integer schoolId, int id);
    Result like(Integer schoolId, long courseId,String name);
}
