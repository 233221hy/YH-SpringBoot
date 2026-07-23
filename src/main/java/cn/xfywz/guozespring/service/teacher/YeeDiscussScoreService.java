package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeDiscussScore;
import cn.xfywz.guozespring.util.Result;

public interface YeeDiscussScoreService {
    Result list(Integer schoolId,Integer discussId,Integer pageSize,Integer pageNum);
    Result update(YeeDiscussScore yeeDiscussScore);
    Result listStudentDiscussScore(Integer schoolId, Integer courseId,Integer discussId, String studentKeyword, Integer classId,
                                   Integer totalPostsMin, Integer postCountMin, Integer replyCountMin, Integer likeCountMin,
                                   Integer scoredStatus, Integer page, Integer pageSize
    );

}
