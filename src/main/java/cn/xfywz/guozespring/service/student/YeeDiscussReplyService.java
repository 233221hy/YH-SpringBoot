package cn.xfywz.guozespring.service.student;

import cn.xfywz.guozespring.entity.mhsch.YeeDiscussReply;
import cn.xfywz.guozespring.util.Result;

/**
 * 课程主题讨论评论回复
 * @TableName yee_discuss_reply
 */
public interface YeeDiscussReplyService {

    Result selectAll(int schoolId, int studentId,int type, int pageSize, int pageNum) throws Exception;

    Result add(YeeDiscussReply yeeDiscussReply,Integer userType) throws Exception;

    Result delete(int id, int schoolId,int userId, int userType) throws Exception;

    Result update(int id, int schoolId, String content) throws Exception;

    Result discussReplyLike(int replyId, int schoolId, int userId);

}
