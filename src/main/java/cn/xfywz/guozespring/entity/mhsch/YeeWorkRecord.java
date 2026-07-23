package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作业记录
 * @TableName yee_work_record
 */
@TableName(value ="yee_work_record")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeWorkRecord {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 作业Id
     */
    private Integer workId;

    /**
     * 学生Id
     */
    private Integer userid;

    /**
     * 开始时间
     */
    private Integer startTime;

    /**
     * 状态
     */
    private Integer state;

    /**
     * 结束时间
     */
    private Integer finishTime;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 取消
     */
    private Integer isCancel;

    /**
     * 次数
     */
    private Integer frequency;

    /**
     * 老师
     */
    private Integer teacherId;

    /**
     * 批阅时间
     */
    private Integer markTime;

    /**
     * 客观题得分
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
     * 平台
     */
    private String platform;

    /**
     * 课程Id
     */
    private Integer courseId;

    /**
     * 互评状态
     */
    private Integer evalState;

    /**
     * 改卷人
     */
    private Integer markId;

    /**
     * 班级Id
     */
    private Integer classId;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     *
     */
    private Date addDate;

    // ========== 新增字段 同步考试 ==========
    /**
     * 交卷类型：0未提交 1手动 2教师强制 3超时自动
     */
    private Integer submitType;

    /**
     * 实际提交时间戳(秒)
     */
    private Integer submitTime;

    /**
     * 最后操作/进入时间戳
     */
    private Integer lastActiveTime;

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
        YeeWorkRecord other = (YeeWorkRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getWorkId() == null ? other.getWorkId() == null : this.getWorkId().equals(other.getWorkId()))
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
                && (this.getEvalState() == null ? other.getEvalState() == null : this.getEvalState().equals(other.getEvalState()))
                && (this.getMarkId() == null ? other.getMarkId() == null : this.getMarkId().equals(other.getMarkId()))
                && (this.getClassId() == null ? other.getClassId() == null : this.getClassId().equals(other.getClassId()))
                && (this.getSchoolId() == null ? other.getSchoolId() == null : this.getSchoolId().equals(other.getSchoolId()))
                && (this.getAddDate() == null ? other.getAddDate() == null : this.getAddDate().equals(other.getAddDate()))
                // 新增字段equals补充
                && (this.getSubmitType() == null ? other.getSubmitType() == null : this.getSubmitType().equals(other.getSubmitType()))
                && (this.getSubmitTime() == null ? other.getSubmitTime() == null : this.getSubmitTime().equals(other.getSubmitTime()))
                && (this.getLastActiveTime() == null ? other.getLastActiveTime() == null : this.getLastActiveTime().equals(other.getLastActiveTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getWorkId() == null) ? 0 : getWorkId().hashCode());
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
        result = prime * result + ((getEvalState() == null) ? 0 : getEvalState().hashCode());
        result = prime * result + ((getMarkId() == null) ? 0 : getMarkId().hashCode());
        result = prime * result + ((getClassId() == null) ? 0 : getClassId().hashCode());
        result = prime * result + ((getSchoolId() == null) ? 0 : getSchoolId().hashCode());
        result = prime * result + ((getAddDate() == null) ? 0 : getAddDate().hashCode());
        // 新增字段hash补充
        result = prime * result + ((getSubmitType() == null) ? 0 : getSubmitType().hashCode());
        result = prime * result + ((getSubmitTime() == null) ? 0 : getSubmitTime().hashCode());
        result = prime * result + ((getLastActiveTime() == null) ? 0 : getLastActiveTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", workid=").append(workId);
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
        sb.append(", evalstate=").append(evalState);
        sb.append(", markid=").append(markId);
        sb.append(", classid=").append(classId);
        sb.append(", schoolid=").append(schoolId);
        sb.append(", adddate=").append(addDate);
        // 新增字段toString
        sb.append(", submitType=").append(submitType);
        sb.append(", submitTime=").append(submitTime);
        sb.append(", lastActiveTime=").append(lastActiveTime);
        sb.append("]");
        return sb.toString();
    }
}