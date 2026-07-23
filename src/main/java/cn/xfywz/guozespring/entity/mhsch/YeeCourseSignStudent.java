package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 课程报名学生
 * @TableName yee_course_sign_student
 */
@TableName(value ="yee_course_sign_student")
@Data
public class YeeCourseSignStudent {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 学生Id
     */
    private Integer studentId;

    /**
     * 课程Id
     */
    private Integer courseId;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     * 报名时间
     */
    private Date signTime;
}