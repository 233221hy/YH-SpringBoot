package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.vo.StuLike;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StudentService {
    Result selectAll(int PageSize, int PageNum,int id) throws Exception;

    Result selectById(int schoolId,int id) throws Exception;
    Result passwordRandom(int schoolId,int id) throws Exception;

    Result passwordReset(int schoolId,List<Integer> id) throws Exception;

    Result selectLikeName(StuLike like) throws Exception;
}
