package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlSchoolBanner {
    private long id;
    private String name;
    private String image;
    private long allow;
    private String link;
    @TableField("schoolId")
    private long schoolId;
    private Integer sort;
}
