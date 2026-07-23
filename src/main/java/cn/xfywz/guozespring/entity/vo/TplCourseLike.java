package cn.xfywz.guozespring.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TplCourseLike {
    private String name;
    private String code;
    @TableField("cateBid")
    private Integer cateBid;
    @TableField("cateMid")
    private Integer cateMid;
    @TableField("schoolId")
    private Integer schoolId;
}