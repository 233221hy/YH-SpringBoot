package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 课程查看记录
 * @TableName yee_course_view_record
 */
@TableName(value ="yee_course_view_record")
@Data
public class YeeCourseViewRecord {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 课程ID
     */
    private Integer courseId;

    /**
     * 用户Id
     */
    private Integer userId;

    /**
     * 次数
     */
    private Integer frequency;

    /**
     * 最后查看时间
     */
    private Date lastTime;

    /**
     * PC查看
     */
    private Integer pcQty;

    /**
     * 移动端查看
     */
    private Integer mbQty;

    /**
     * 学校Id
     */
    private Integer schoolId;
}