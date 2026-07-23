package cn.xfywz.guozespring.service.student;

import cn.xfywz.guozespring.entity.mhsch.YeeSignInRecord;
import cn.xfywz.guozespring.entity.dto.YeeSignInRecordQuery;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

public interface YeeSignInRecordService {

    void add(YeeSignInRecord param);

    Result selectById(int schoolId, long id) throws Exception;

    Result list(YeeSignInRecordQuery param) throws Exception;

    void update(YeeSignInRecord param);

    void exportData(YeeSignInRecordQuery queryDTO, HttpServletResponse response) throws Exception;
}
