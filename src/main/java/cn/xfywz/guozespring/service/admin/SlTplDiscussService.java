package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplDiscuss;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlTplDiscussService {
    Result showAll(int pageSize, int pageNum, int courseId);

    Result add(SlTplDiscuss slTplDiscuss);

    Result del(int id);

    Result update(SlTplDiscuss slTplDiscuss);
}
