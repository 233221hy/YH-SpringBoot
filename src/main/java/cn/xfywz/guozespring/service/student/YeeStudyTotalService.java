
package cn.xfywz.guozespring.service.student;
        import cn.xfywz.guozespring.entity.dto.*;
        import cn.xfywz.guozespring.util.Result;

/**
 * 学生学习记录服务接口
 */
public interface YeeStudyTotalService {
    /**
     * 保存学生看课节点记录
     * @param schoolId 学校ID
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param courseId 课程ID
     * @param duration 学习时长(秒)
     * @param progress 学习进度(0-100)
     * @param ip 学习IP地址
     * @param terminal 学习终端
     * @return 保存结果
     */
    Result saveStudyRecord(int schoolId, int userId, int nodeId, int courseId,
                           int duration, String progress, String ip, String terminal) throws Exception;

    /**
     * 获取用户在某个课程的学习进度统计
     * @param schoolId 学校ID
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 学习进度统计
     */
    Result getStudyProgress(int schoolId, int userId, int courseId) throws Exception;
    Result startSession(StudySessionStartDTO dto,String clientIp);
    Result heartbeat(StudySessionHeartbeatDTO dto);
    Result saveStudyRecordFromSession(StudySession session, String finalProgress) throws Exception;
    Result endSession(StudySessionEndDTO dto);
    Result getLastProgressStr(int userId, int nodeId, int schoolId);
}
