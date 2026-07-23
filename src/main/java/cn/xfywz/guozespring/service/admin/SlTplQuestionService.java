package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplQuestion;
import cn.xfywz.guozespring.entity.vo.SlTplQuestionLike;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlTplQuestionService {
    Result selectAll(int PageSize, int PageNum);
    Result add(SlTplQuestion slTplQuestion);
    Result del(int id);
    Result update(SlTplQuestion slTplQuestion);
    Result search(SlTplQuestionLike slTplQuestionLike);
}
