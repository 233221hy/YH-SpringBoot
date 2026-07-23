package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.util.Result;

public interface YeeNodeDiscussService {

    // 添加评论
    Result add(cn.xfywz.guozespring.entity.mhsch.YeeNodeDiscuss yeeNodeDiscuss) throws Exception;
    // 更新评论
    Result update(int id, int schoolId, String content) throws Exception;
    // 删除评论
    Result delete(int id, int schoolId) throws Exception;
    // 点赞/取消点赞
    Result yeeNodeReplyLike(int id, int schoolId, int userId);
    // 节点讨论列表
    Result discussList(int pageNum, int pageSize, int schoolId, long userId, int nodeId) throws Exception;

}
