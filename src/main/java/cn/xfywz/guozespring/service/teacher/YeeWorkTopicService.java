package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeWorkTopic;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

public interface YeeWorkTopicService {
    Result selectAll(int schoolId, Integer pageSize, Integer pageNum, Integer workId, String topic, Integer type, Integer level, Integer cateBid, Integer cateMid) throws Exception;
    Result add(YeeWorkTopic yeeWorkTopic) throws Exception;
    Result update(YeeWorkTopic yeeWorkTopic) throws Exception;
    Result delete(int schoolId, int id) throws Exception;

    Result getById(int schoolId, int id) throws Exception;

    Result sortByNumber(int schoolId, int id1, int id2, int number1, int number2) throws Exception;
    
    void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid, Integer workId) throws Exception;
}
