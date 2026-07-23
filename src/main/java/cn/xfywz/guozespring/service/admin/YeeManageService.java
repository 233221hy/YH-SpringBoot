package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.dto.YeeManageQueryParam;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.vo.YeeManageLike;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public interface YeeManageService {
//    Result selectAll(int schoolId, int pageSize, int pageNum) throws Exception;

    Result selectAll(YeeManageQueryParam param);

    Result selectById(int schoolId, int id) throws Exception;

    Result deleteById(int schoolId, int id);

    Result update(YeeManage yeeManage);

    Result lock(int schoolId, int id);

    Result active(int schoolId, int id);

    Result searchByCondition(YeeManageLike yeeManageLike);

    Result actives(int schoolId, ArrayList<Integer> id) throws Exception;

    Result password(int schoolId, ArrayList<Integer> id) throws Exception;

    Result add(YeeManage yeeManage) throws Exception;

    void updateLoginInfo(YeeManage updateDto);
}
