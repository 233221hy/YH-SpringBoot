package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeMutedQueryParam {
    private Integer pageNum;
    private Integer pageSize;
    private Integer schoolId;
    private String name;
    private String idCard;
}
