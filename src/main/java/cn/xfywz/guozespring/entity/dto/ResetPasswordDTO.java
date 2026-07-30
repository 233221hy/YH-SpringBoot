package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDTO {
    private String stuNumber;   //学生学号
    private String email;       //学生邮箱
//    private String idCardLast6;
//    private String newPassword;
}
