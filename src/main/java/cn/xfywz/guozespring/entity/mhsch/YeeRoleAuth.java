package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("yee_role_auth")
public class YeeRoleAuth {
  @TableId(type = IdType.AUTO)
  private long id;
  private long roleId;
  private long authId;
  private long schoolId;
}
