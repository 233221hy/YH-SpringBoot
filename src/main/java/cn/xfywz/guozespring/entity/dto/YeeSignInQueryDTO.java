package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeSignInQueryDTO {
    // 分页参数
    private Integer pageNum;
    private Integer pageSize;
    // 必传参数
    private int schoolId;
    private int courseId;

}
