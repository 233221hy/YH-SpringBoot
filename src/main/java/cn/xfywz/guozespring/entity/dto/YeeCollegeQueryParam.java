package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCollegeQueryParam {
    private Integer pageNum = 1;
    private Integer pageSize;
    private Integer id;
    private String name;
    private Integer allow;

}
