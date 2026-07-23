package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课程报名班级
 * @TableName yee_course_sign_class
 */
@TableName(value ="yee_course_sign_class")
@Data
public class YeeCourseSignClass {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 课程Id
     */
    private Integer courseId;

    /**
     * 班级Id
     */
    private Integer classId;

    /**
     * 学校Id
     */
    private Integer schoolId;
}