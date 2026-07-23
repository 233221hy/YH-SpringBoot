package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenAnnouncement;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlOpenAnnouncementService {
    Result select(Integer id);
    Result add(SlOpenAnnouncement slOpenAnnouncement);
    Result update(SlOpenAnnouncement slOpenAnnouncement);
    Result delete(Integer id);
}
