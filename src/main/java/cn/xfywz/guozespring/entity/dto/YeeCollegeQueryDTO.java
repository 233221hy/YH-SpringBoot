package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeCollegeQueryDTO {
    // 分页参数
    private Integer pageNum = 1;
    private Integer pageSize = 999;

    private Integer schoolId;

    // 查询参数
    private String name;
    private Integer allow;
}
