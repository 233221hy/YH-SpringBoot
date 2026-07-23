package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeStudyTotal {
    private long id;
    private long nodeId;  //节点id
    private long userId; //用户id
    private long duration; //学习时长
    private String progress; //学习进度
    private long courseId; //课程id
    private Integer state; //学习状态
    private long times; //学习次数
    private long finalTime; //最后完成时间
    private long schoolId; //学校id
}
