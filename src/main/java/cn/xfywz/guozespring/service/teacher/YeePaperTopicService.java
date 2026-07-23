package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeePaperTopic;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface YeePaperTopicService {
    Result selectAll(int schoolId, Integer pageSize, Integer pageNum, Integer paperId, String topic, Integer type, Integer level, Integer cateBid, Integer cateMid) throws Exception;
    Result add(YeePaperTopic yeePaperTopic) throws Exception;
    Result update(YeePaperTopic yeePaperTopic) throws Exception;
    Result delete(int schoolId, int id) throws Exception;

    Result getById(int schoolId, int id) throws Exception;

    Result sortByNumber(int schoolId, int id1, int id2, int number1, int number2) throws Exception;

    /**
     * 导出试题数据为Excel
     * @param response
     * @param schoolId
     * @param topic
     * @param createId
     * @param type
     * @param level
     * @param cateBid
     * @param cateMid
     */
    void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid, Integer paperId) throws Exception;

    /**
     * 从Excel导入试题数据
     * @param schoolId
     * @param createId
     * @param file
     * @param cateBid
     * @param cateMid
     * @return
     */
    Result importQuestions(Integer schoolId, Integer createId, MultipartFile file, Integer cateBid, Integer cateMid, Integer paperId) throws Exception;
}
