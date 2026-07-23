package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 作业分数
 * @TableName yee_work_score
 */
@TableName(value ="yee_work_score")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeWorkScore {
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
    private Integer userId;

    /**
     * 最终得分
     */
    private BigDecimal finalScore;

    /**
     * 状态
     */
    private Integer state;

    /**
     * 已打分
     */
    private Boolean scored;

    /**
     * 提交时间
     */
    private Integer submitTime;

    /**
     * 用时
     */
    private Integer timeCost;

    /**
     * 平台
     */
    private String platform;

    /**
     * 课程ID
     */
    private Integer courseId;

    /**
     * 学校Id
     */
    private Integer schoolId;

    public Boolean getScored() {
        return scored;
    }

    public void setScored(Boolean scored) {
        this.scored = scored;
    }
}