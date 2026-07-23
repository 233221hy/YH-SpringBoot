package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.util.Result;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface YeeQuestionService {
    Result selectAll(int schoolId, Integer pageSize, Integer pageNum, String topic, Integer createId, String creatorName, Integer type, Integer level, Integer cateBid, Integer cateMid) throws Exception;

    Result add(YeeQuestion yeeQuestion) throws Exception;
    Result update(YeeQuestion yeeQuestion) throws Exception;
    Result delete(int schoolId, int id) throws Exception;
    
    // 批量删除方法
    Result batchDelete(int schoolId, List<Integer> ids) throws Exception;

    Result getById(int schoolId, int id) throws Exception;
    
    /**
     * 从Excel导入试题数据
     * @param schoolId 学校ID
     * @param createId 创建人ID
     * @param file Excel文件
     * @return 导入结果
     * @throws Exception 数据库操作异常
     */
    Result importQuestions(int schoolId, int createId, MultipartFile file, Integer cateBid, Integer cateMid) throws Exception;
}