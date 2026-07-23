package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeHappyCircle;
import cn.xfywz.guozespring.util.Result;

public interface YeeHappyCService {

    
    // 主评论列表
    Result list(int pageNum, int pageSize, int schoolId, long userId) throws Exception;
    
    Result detail(int schoolId, long id) throws Exception;
    Result add(YeeHappyCircle circle) throws Exception;
    Result delete(long id, int schoolId) throws Exception;


    // 点赞（存在则取消，返回点赞状态）
    Result likeToggle(int schoolId, long replyId, long userId) throws Exception;

    // 回复相关
    Result addReply(YeeHappyCircle reply) throws Exception;

}