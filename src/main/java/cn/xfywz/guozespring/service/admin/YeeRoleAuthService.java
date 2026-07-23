package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhsch.YeeRoleAuth;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface YeeRoleAuthService {

    Result roleAuth_list(long schoolId, int pageSize, int pageNum) throws Exception;

    Result roleAuth_add(YeeRoleAuth yeeRoleAuth) throws Exception;

    Result roleAuth_update(YeeRoleAuth yeeRoleAuth) throws Exception;

    Result roleAuth_delete(long id) throws Exception;

    Result roleAuth_delete_by_role(long roleId, long schoolId) throws Exception;

    Result roleAuth_search_by_role(long roleId, long schoolId) throws Exception;

    Result roleAuth_search_by_auth(long authId, long schoolId) throws Exception;

    Result roleAuth_batch_add(long roleId, long schoolId, List<Long> authIds) throws Exception;
}