package cn.xfywz.guozespring.entity.mhsch;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试卷实体类
 * 对应数据库表：yee_paper
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeePaper implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer userId;
    private String title;
    private Integer topicNumber;
    private Integer score;
    private Integer type;
    private String scope;
    private String remarks;
    private Byte allow;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime addTime;
    private Integer schoolId;
    private List<Integer> categoryId;
    private Integer cateBid;
    private Integer cateMid;
    private LocalDate addDate;
}