package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseResultsQueryDTO {
    private Integer pageNum;
    private Integer pageSize;
    private Integer schoolId;
    private long courseId;
    private long classId;
    private String keyword;
}
