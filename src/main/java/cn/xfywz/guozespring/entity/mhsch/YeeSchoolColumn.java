package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName yee_school_column
 */
@TableName(value ="yee_school_column")
@Data
public class YeeSchoolColumn {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 栏目名称
     */
    private String name;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 更多链接
     */
    private String more;

    /**
     * 审核
     */
    private Integer allow;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 
     */
    private Object data;

    /**
     * 
     */
    private Date addTime;

    /**
     * 
     */
    private Integer schoolId;
}