package cn.xfywz.guozespring.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseQueryParam {
    // 分页参数
    private Integer pageSize = 10;
    private Integer pageNum = 1;
    
    // 原LikeYeeCourse字段
    private Integer id;
    private String name;
    private Integer mode;
    private Integer collegeId;
    private String code;
    private Integer createId;
    private Integer schoolId;
    private Integer cateBid;
    private Integer cateMid;
    
    // 新增的查询条件
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime  startDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")// 开课时间
    private LocalDateTime   endDate;          // 结课时间
    private String cover;
    private BigDecimal credit;
    private Integer allow;         // 是否发布
    private Integer stuCount;
    private Integer clusterId;
    private String periodName;
    private String lecturerName;
    
    // 特殊查询条件
    private Boolean selfBuilt;     // 是否自建 (tplId为0表示自建)
    private Integer allowQuery;    // 是否发布查询 (避免与已有的allow字段冲突，如果需要的话)
    
    // 关联表查询条件
    private String createName;     // 创建者名称
    private String collegeName;    // 学院名称
}
