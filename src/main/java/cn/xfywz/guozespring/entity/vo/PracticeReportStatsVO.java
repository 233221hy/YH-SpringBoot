package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeReportStatsVO {
    private Integer totalStudents;
    private Integer notSubmitted;
    private Integer submitted;
    private Integer passed;
    private Integer notPassed;
}