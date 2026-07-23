package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkScoreMessage implements Serializable {
    private int schoolId;
    private Integer recordId;
    private Integer workId;
    private Integer topicId;
    private Integer userId;
    private Integer courseId;
    private BigDecimal earnedScore;
}
