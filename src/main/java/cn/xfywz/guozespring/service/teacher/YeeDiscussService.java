package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.BatchDiscussScoreReq;
import cn.xfywz.guozespring.entity.mhsch.YeeDiscuss;
import cn.xfywz.guozespring.util.Result;

import java.util.List;

public interface YeeDiscussService {
    Result list(int pageNum, int pageSize, int schoolId, long courseId) throws Exception;
    Result add(YeeDiscuss yeeDiscuss) throws Exception;
    Result update(YeeDiscuss yeeDiscuss) throws Exception;
    Result delete(long id, int schoolId) throws Exception;
    Result like(int schoolId, long courseId, String title);
    Result batchUpdateScore(Integer schoolId, Long discussId, List<BatchDiscussScoreReq.ScoreItem> scores);
}
