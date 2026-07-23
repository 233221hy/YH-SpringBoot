package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeRoleAuthBatch {
    private long roleId;
    private long schoolId;
    private List<Long> authIds;
}