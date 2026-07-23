package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeClassesQueryParam {
    private Integer pageNum = 1;
    private Integer pageSize;
    private Integer id;
    private String name;
    private Integer collegeId;
    private Integer allow;
    // 学校ID（鉴权与数据源路由所需）
    private Integer schoolId;
}
