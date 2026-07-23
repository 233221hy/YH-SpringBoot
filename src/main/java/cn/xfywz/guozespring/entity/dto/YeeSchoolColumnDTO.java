package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeSchoolColumnDTO {
    private long id;
    private long schoolId;

    //条件
    private String name;
    private Integer allow;
}
