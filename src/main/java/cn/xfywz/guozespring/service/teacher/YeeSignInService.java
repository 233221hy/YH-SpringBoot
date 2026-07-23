package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeSignIn;
import cn.xfywz.guozespring.util.Result;

public interface YeeSignInService {
    Result listSignIn(int schoolId,int courseId,int pageSize,int pageNum);
    Result addSignIn(YeeSignIn yeeSignIn);
    Result delSignIn(int id,int schoolId);
    Result likeSignIn(int schoolId,int courseId,String name);
    Result updateSignIn(YeeSignIn yeeSignIn);
}
