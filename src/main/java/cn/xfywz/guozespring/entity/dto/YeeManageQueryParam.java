package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeManageQueryParam {
    private Integer schoolId;
    private Integer pageNum;
    private Integer pageSize;
    private Integer collegeId;
    private Integer recommend;
    private Integer active;
    private Integer isLock;
    private Integer role;
    private String account;
    private String name;

}
