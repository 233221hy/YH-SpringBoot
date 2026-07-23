package cn.xfywz.guozespring.entity.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscussScoreDto {

    private Integer id;
    private Integer rank;
    private Integer studentId;
    private String studentName;
    private String studentNumber;
    private String className;
    private Integer allQty;
    private Integer postQty;
    private Integer replyQty;
    private Integer likeQty;
    private String status;
    private BigDecimal score;
}
