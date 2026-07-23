package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyRecordQuery {
    //分页
    private Integer pageNum;
    private Integer pageSize;

    //必填
    private Integer schoolId;
    private long courseId;

    private long classId;
    private long studentId;
//    private String videoProgress;  // 视频进度 (已学/总数)
//    private String workProgress;   // 作业进度 (已学/总数)
//    private String examProgress;   // 测验进度 (已学/总数)
//    private String discussProgress;// 讨论进度 (参与/总数)
//    private String studyTime;//学习时长
    private String keyword; //搜索关键字
    private Integer state;

    // 退回重学原因（可选）
    private String content;
}
