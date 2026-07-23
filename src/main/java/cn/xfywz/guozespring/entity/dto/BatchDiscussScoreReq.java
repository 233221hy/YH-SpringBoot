package cn.xfywz.guozespring.entity.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchDiscussScoreReq {

    private Integer schoolId;      // 必填
    private Long discussId;        // 必填（讨论主题ID）
    private List<ScoreItem> scores; // 至少1项

    @Data
    public static class ScoreItem {
        private Integer userId;     // 学生ID
        private BigDecimal score;   // 分数（0-100）
    }
}
