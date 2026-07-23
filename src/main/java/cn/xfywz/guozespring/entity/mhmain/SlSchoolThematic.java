package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlSchoolThematic {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 专题名称
     */
    private String name;

    /**
     * 封面背景
     */
    private String background;

    /**
     * 专题标签（JSON格式）
     */
    @TableField(value = "tags")
    private String tags;

    /**
     * 关联课程1
     */
    @TableField(value = "courseId1")
    private Integer courseId1;

    /**
     * 关联课程2
     */
    @TableField(value = "courseId2")
    private Integer courseId2;

    /**
     * 关联课程3
     */
    @TableField(value = "courseId3")
    private Integer courseId3;

    /**
     * 关联课程4
     */
    @TableField(value = "courseId4")
    private Integer courseId4;

    /**
     * 关联课程5
     */
    @TableField(value = "courseId5")
    private Integer courseId5;

    /**
     * 关联课程6
     */
    @TableField(value = "courseId6")
    private Integer courseId6;

    /**
     * 是否审核（0:未审核, 1:已审核）
     */
    private Integer allow;

    /**
     * 排序权重
     */
    private Integer sort;

    /**
     * 所属学校ID
     */
    @TableField(value = "schoolId")
    private Integer schoolId;
}
