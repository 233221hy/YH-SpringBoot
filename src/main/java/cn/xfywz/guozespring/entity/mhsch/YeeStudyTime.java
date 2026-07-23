package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeStudyTime {
    private long id;
    private long nodeId;//节点id
    private long userId;//用户id
    private long duration;//学习时长
    private Timestamp addTime;//学习时间
    private String ip;//学习ip
    private String terminal;//学习终端
    private long courseId;//课程id
    private long beginTime;//开始时间
    private long lastTime;//最后活跃时间
    private long schoolId;//学校id
    private long post;//更新次数
    private Integer close;//有无关闭
    private Integer wg;//检测外挂
    private Date addDate;
}
