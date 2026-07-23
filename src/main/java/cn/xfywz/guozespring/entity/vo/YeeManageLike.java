package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeManageLike {
    Long schoolId;
    String like;
    Integer role;
    Integer isLock;
    Integer isActive;

}
