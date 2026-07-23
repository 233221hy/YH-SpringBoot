package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 考试记录
 * @TableName yee_exam_record
 */
@TableName(value ="yee_exam_record")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeExamRecord {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 考试Id
     */
    private Integer examId;

    /**
     * 学生Id
     */
    private Integer userid;

    /**
     * 开始时间（秒时间戳）
     */
    private Integer startTime;

    /**
     * 是否结束：1未提交 2待批阅 3已批阅
     */
    private Integer state;

    /**
     * 结束时间（秒时间戳）
     */
    private Integer finishTime;

    /**
     * 总分
     */
    private BigDecimal score;

    /**
     * 取消 0正常 1作废
     */
    private Integer isCancel;

    /**
     * 答题次数
     */
    private Integer frequency;

    /**
     * 教师id
     */
    private Integer teacherId;

    /**
     * 批阅时间戳
     */
    private Integer markTime;

    /**
     * 客观得分
     */
    private BigDecimal obScore;

    /**
     * 主观题得分
     */
    private BigDecimal subScore;

    /**
     * 改卷顺序
     */
    private Integer markOrder;

    /**
     * 平台标识
     */
    private String platform;

    /**
     * 课程Id
     */
    private Integer courseId;

    /**
     * 班级Id
     */
    private Integer classId;

    /**
     * 学校Id
     */
    private Integer schoolId;

    private Integer redo;

    /**
     * 日期虚拟字段
     */
    private Date addDate;

    /**
     * 交卷类型：0未提交 1学生手动交卷 2教师一键强制收卷 3考试超时系统自动收卷
     */
    private Integer submitType;

    /**
     * 交卷时间戳(秒)，未交卷为0
     */
    private Integer submitTime;

    /**
     * 学生最后进入/操作时间戳(秒)
     */
    private Integer lastActiveTime;

    /**
     * 本场抽题原始ID顺序JSON，兜底兼容无answer数据场景
     */
    private String selectTopicIds;


    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        YeeExamRecord other = (YeeExamRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getExamId() == null ? other.getExamId() == null : this.getExamId().equals(other.getExamId()))
                && (this.getUserid() == null ? other.getUserid() == null : this.getUserid().equals(other.getUserid()))
                && (this.getStartTime() == null ? other.getStartTime() == null : this.getStartTime().equals(other.getStartTime()))
                && (this.getState() == null ? other.getState() == null : this.getState().equals(other.getState()))
                && (this.getFinishTime() == null ? other.getFinishTime() == null : this.getFinishTime().equals(other.getFinishTime()))
                && (this.getScore() == null ? other.getScore() == null : this.getScore().equals(other.getScore()))
                && (this.getIsCancel() == null ? other.getIsCancel() == null : this.getIsCancel().equals(other.getIsCancel()))
                && (this.getFrequency() == null ? other.getFrequency() == null : this.getFrequency().equals(other.getFrequency()))
                && (this.getTeacherId() == null ? other.getTeacherId() == null : this.getTeacherId().equals(other.getTeacherId()))
                && (this.getMarkTime() == null ? other.getMarkTime() == null : this.getMarkTime().equals(other.getMarkTime()))
                && (this.getObScore() == null ? other.getObScore() == null : this.getObScore().equals(other.getObScore()))
                && (this.getSubScore() == null ? other.getSubScore() == null : this.getSubScore().equals(other.getSubScore()))
                && (this.getMarkOrder() == null ? other.getMarkOrder() == null : this.getMarkOrder().equals(other.getMarkOrder()))
                && (this.getPlatform() == null ? other.getPlatform() == null : this.getPlatform().equals(other.getPlatform()))
                && (this.getCourseId() == null ? other.getCourseId() == null : this.getCourseId().equals(other.getCourseId()))
                && (this.getClassId() == null ? other.getClassId() == null : this.getClassId().equals(other.getClassId()))
                && (this.getSchoolId() == null ? other.getSchoolId() == null : this.getSchoolId().equals(other.getSchoolId()))
                && (this.getRedo() == null ? other.getRedo() == null : this.getRedo().equals(other.getRedo()))
                && (this.getAddDate() == null ? other.getAddDate() == null : this.getAddDate().equals(other.getAddDate()))
                && (this.getSubmitType() == null ? other.getSubmitType() == null : this.getSubmitType().equals(other.getSubmitType()))
                && (this.getSubmitTime() == null ? other.getSubmitTime() == null : this.getSubmitTime().equals(other.getSubmitTime()))
                && (this.getLastActiveTime() == null ? other.getLastActiveTime() == null : this.getLastActiveTime().equals(other.getLastActiveTime()))
                && (this.getSelectTopicIds() == null ? other.getSelectTopicIds() == null : this.getSelectTopicIds().equals(other.getSelectTopicIds()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getExamId() == null) ? 0 : getExamId().hashCode());
        result = prime * result + ((getUserid() == null) ? 0 : getUserid().hashCode());
        result = prime * result + ((getStartTime() == null) ? 0 : getStartTime().hashCode());
        result = prime * result + ((getState() == null) ? 0 : getState().hashCode());
        result = prime * result + ((getFinishTime() == null) ? 0 : getFinishTime().hashCode());
        result = prime * result + ((getScore() == null) ? 0 : getScore().hashCode());
        result = prime * result + ((getIsCancel() == null) ? 0 : getIsCancel().hashCode());
        result = prime * result + ((getFrequency() == null) ? 0 : getFrequency().hashCode());
        result = prime * result + ((getTeacherId() == null) ? 0 : getTeacherId().hashCode());
        result = prime * result + ((getMarkTime() == null) ? 0 : getMarkTime().hashCode());
        result = prime * result + ((getObScore() == null) ? 0 : getObScore().hashCode());
        result = prime * result + ((getSubScore() == null) ? 0 : getSubScore().hashCode());
        result = prime * result + ((getMarkOrder() == null) ? 0 : getMarkOrder().hashCode());
        result = prime * result + ((getPlatform() == null) ? 0 : getPlatform().hashCode());
        result = prime * result + ((getCourseId() == null) ? 0 : getCourseId().hashCode());
        result = prime * result + ((getClassId() == null) ? 0 : getClassId().hashCode());
        result = prime * result + ((getSchoolId() == null) ? 0 : getSchoolId().hashCode());
        result = prime * result + ((getRedo() == null) ? 0 : getRedo().hashCode());
        result = prime * result + ((getAddDate() == null) ? 0 : getAddDate().hashCode());
        result = prime * result + ((getSubmitType() == null) ? 0 : getSubmitType().hashCode());
        result = prime * result + ((getSubmitTime() == null) ? 0 : getSubmitTime().hashCode());
        result = prime * result + ((getLastActiveTime() == null) ? 0 : getLastActiveTime().hashCode());
        result = prime * result + ((getSelectTopicIds() == null) ? 0 : getSelectTopicIds().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", examid=").append(examId);
        sb.append(", userid=").append(userid);
        sb.append(", starttime=").append(startTime);
        sb.append(", state=").append(state);
        sb.append(", finishtime=").append(finishTime);
        sb.append(", score=").append(score);
        sb.append(", iscancel=").append(isCancel);
        sb.append(", frequency=").append(frequency);
        sb.append(", teacherid=").append(teacherId);
        sb.append(", marktime=").append(markTime);
        sb.append(", obscore=").append(obScore);
        sb.append(", subscore=").append(subScore);
        sb.append(", markorder=").append(markOrder);
        sb.append(", platform=").append(platform);
        sb.append(", courseid=").append(courseId);
        sb.append(", classid=").append(classId);
        sb.append(", schoolid=").append(schoolId);
        sb.append(", redo=").append(redo);
        sb.append(", adddate=").append(addDate);
        sb.append(", submitType=").append(submitType);
        sb.append(", submitTime=").append(submitTime);
        sb.append(", lastActiveTime=").append(lastActiveTime);
        sb.append(", selectTopicIds=").append(selectTopicIds);
        sb.append("]");
        return sb.toString();
    }
}