package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeStudentQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize;
    private Integer schoolId;
    private String keyword;
    private String idCard;
    private String gender;
    private Integer entryYear;
    private Integer collegeId;
    private Integer classId;
}
