package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.annotation.Order;

/**
 * 作业
 * @TableName yee_work
 */
@Order(-1)
@TableName(value ="yee_work")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeWork {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户Id
     */
    private Integer userId;

    /**
     * 作业标题
     */
    private String title;

    /**
     * 题目数量
     */
    private Integer topicNumber;

    /**
     * 总分数
     */
    private Integer score;

    /**
     * 测验类型
     */
    private Integer type;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date addTime;

    /**
     * 试题顺序
     */
    private Integer sequence;

    /**
     * 所在节点
     */
    private Integer nodeId;

    /**
     * 所在课程
     */
    private Integer courseId;

    /**
     * 开始时间
     */
    private Integer startTime;

    /**
     * 结束时间
     */
    private Integer endTime;

    /**
     * 选择试卷
     */
    private Integer paperId;

    /**
     * 创建人
     */
    private Integer createUserId;

    /**
     * 适用范围
     */
    private Integer isPrivate;

    /**
     * 选择班级
     */
    private Object classList;

    /**
     * 老师类型
     */
    private Integer teacherType;

    /**
     * 是否启用
     */
    private Integer allow;

    /**
     * 答题次数
     */
    private Integer frequency;

    /**
     * 成绩规则
     */
    private Integer scoringRules;

    /**
     * 已有收卷
     */
    private Integer hasCollect;

    /**
     * 锁定
     */
    private Integer lock;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     * 显示解析
     */
    private Integer parsing;

    /**
     * 
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date addDate;

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
        YeeWork other = (YeeWork) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getTopicNumber() == null ? other.getTopicNumber() == null : this.getTopicNumber().equals(other.getTopicNumber()))
            && (this.getScore() == null ? other.getScore() == null : this.getScore().equals(other.getScore()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getRemarks() == null ? other.getRemarks() == null : this.getRemarks().equals(other.getRemarks()))
            && (this.getAddTime() == null ? other.getAddTime() == null : this.getAddTime().equals(other.getAddTime()))
            && (this.getSequence() == null ? other.getSequence() == null : this.getSequence().equals(other.getSequence()))
            && (this.getNodeId() == null ? other.getNodeId() == null : this.getNodeId().equals(other.getNodeId()))
            && (this.getCourseId() == null ? other.getCourseId() == null : this.getCourseId().equals(other.getCourseId()))
            && (this.getStartTime() == null ? other.getStartTime() == null : this.getStartTime().equals(other.getStartTime()))
            && (this.getEndTime() == null ? other.getEndTime() == null : this.getEndTime().equals(other.getEndTime()))
            && (this.getPaperId() == null ? other.getPaperId() == null : this.getPaperId().equals(other.getPaperId()))
            && (this.getCreateUserId() == null ? other.getCreateUserId() == null : this.getCreateUserId().equals(other.getCreateUserId()))
            && (this.getIsPrivate() == null ? other.getIsPrivate() == null : this.getIsPrivate().equals(other.getIsPrivate()))
            && (this.getClassList() == null ? other.getClassList() == null : this.getClassList().equals(other.getClassList()))
            && (this.getTeacherType() == null ? other.getTeacherType() == null : this.getTeacherType().equals(other.getTeacherType()))
            && (this.getAllow() == null ? other.getAllow() == null : this.getAllow().equals(other.getAllow()))
            && (this.getFrequency() == null ? other.getFrequency() == null : this.getFrequency().equals(other.getFrequency()))
            && (this.getScoringRules() == null ? other.getScoringRules() == null : this.getScoringRules().equals(other.getScoringRules()))
            && (this.getHasCollect() == null ? other.getHasCollect() == null : this.getHasCollect().equals(other.getHasCollect()))
            && (this.getLock() == null ? other.getLock() == null : this.getLock().equals(other.getLock()))
            && (this.getSchoolId() == null ? other.getSchoolId() == null : this.getSchoolId().equals(other.getSchoolId()))
            && (this.getParsing() == null ? other.getParsing() == null : this.getParsing().equals(other.getParsing()))
            && (this.getAddDate() == null ? other.getAddDate() == null : this.getAddDate().equals(other.getAddDate()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getTopicNumber() == null) ? 0 : getTopicNumber().hashCode());
        result = prime * result + ((getScore() == null) ? 0 : getScore().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getRemarks() == null) ? 0 : getRemarks().hashCode());
        result = prime * result + ((getAddTime() == null) ? 0 : getAddTime().hashCode());
        result = prime * result + ((getSequence() == null) ? 0 : getSequence().hashCode());
        result = prime * result + ((getNodeId() == null) ? 0 : getNodeId().hashCode());
        result = prime * result + ((getCourseId() == null) ? 0 : getCourseId().hashCode());
        result = prime * result + ((getStartTime() == null) ? 0 : getStartTime().hashCode());
        result = prime * result + ((getEndTime() == null) ? 0 : getEndTime().hashCode());
        result = prime * result + ((getPaperId() == null) ? 0 : getPaperId().hashCode());
        result = prime * result + ((getCreateUserId() == null) ? 0 : getCreateUserId().hashCode());
        result = prime * result + ((getIsPrivate() == null) ? 0 : getIsPrivate().hashCode());
        result = prime * result + ((getClassList() == null) ? 0 : getClassList().hashCode());
        result = prime * result + ((getTeacherType() == null) ? 0 : getTeacherType().hashCode());
        result = prime * result + ((getAllow() == null) ? 0 : getAllow().hashCode());
        result = prime * result + ((getFrequency() == null) ? 0 : getFrequency().hashCode());
        result = prime * result + ((getScoringRules() == null) ? 0 : getScoringRules().hashCode());
        result = prime * result + ((getHasCollect() == null) ? 0 : getHasCollect().hashCode());
        result = prime * result + ((getLock() == null) ? 0 : getLock().hashCode());
        result = prime * result + ((getSchoolId() == null) ? 0 : getSchoolId().hashCode());
        result = prime * result + ((getParsing() == null) ? 0 : getParsing().hashCode());
        result = prime * result + ((getAddDate() == null) ? 0 : getAddDate().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userid=").append(userId);
        sb.append(", title=").append(title);
        sb.append(", topicnumber=").append(topicNumber);
        sb.append(", score=").append(score);
        sb.append(", type=").append(type);
        sb.append(", remarks=").append(remarks);
        sb.append(", addtime=").append(addTime);
        sb.append(", sequence=").append(sequence);
        sb.append(", nodeid=").append(nodeId);
        sb.append(", courseid=").append(courseId);
        sb.append(", starttime=").append(startTime);
        sb.append(", endtime=").append(endTime);
        sb.append(", paperid=").append(paperId);
        sb.append(", createuserid=").append(createUserId);
        sb.append(", isprivate=").append(isPrivate);
        sb.append(", classlist=").append(classList);
        sb.append(", teachertype=").append(teacherType);
        sb.append(", allow=").append(allow);
        sb.append(", frequency=").append(frequency);
        sb.append(", scoringrules=").append(scoringRules);
        sb.append(", hascollect=").append(hasCollect);
        sb.append(", lock=").append(lock);
        sb.append(", schoolid=").append(schoolId);
        sb.append(", parsing=").append(parsing);
        sb.append(", adddate=").append(addDate);
        sb.append("]");
        return sb.toString();
    }
}

