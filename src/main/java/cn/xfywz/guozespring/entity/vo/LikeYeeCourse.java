package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeYeeCourse {
    private String name;
    private Integer mod;
    private Integer collegeId;
    private Integer cateBid;
    private Integer cateMid;
    private String code;
    private Integer createId;
    private Integer schoolId;
    private String createName;
    private LocalDate startDate;
    private LocalDate endDate;
}
