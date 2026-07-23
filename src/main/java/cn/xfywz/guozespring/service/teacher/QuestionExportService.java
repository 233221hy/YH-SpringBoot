package cn.xfywz.guozespring.service.teacher;

import jakarta.servlet.http.HttpServletResponse;

public interface QuestionExportService {

    public void exportQuestions(HttpServletResponse response, Integer schoolId, String topic,
                                Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid) throws Exception;

}
