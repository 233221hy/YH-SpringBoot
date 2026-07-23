package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Data
public class YeeCourseClassVo {


    private Integer id;
    private String name;           // 教学班级名称
    private Integer courseId;
    private Integer teacherId;
    private String teacherName;    // 责任教师姓名（来自 yee_manage）
    private Integer schoolId;
    private Byte allow;
    private Date addTime;
    private Integer createId;
    private Byte change;
    private Byte calculate;

    private Integer studentNum;    // 该班级选课学生人数
    private Integer announce;//公布成绩状态

    public static YeeCourseClassVo rsToCourseClassWithStats(ResultSet rs) throws SQLException {
        YeeCourseClassVo obj = new YeeCourseClassVo();

        obj.setId(rs.getInt("id"));
        obj.setName(rs.getString("name"));
        obj.setCourseId(rs.getInt("courseId"));
        obj.setTeacherId(rs.getObject("teacherId") != null ? rs.getInt("teacherId") : null);
        obj.setTeacherName(rs.getString("teacherName")); // 可能为 null
        obj.setSchoolId(rs.getInt("schoolId"));
        obj.setAllow(rs.getByte("allow"));
        obj.setAddTime(rs.getTimestamp("addTime"));
        obj.setCreateId(rs.getObject("createId") != null ? rs.getInt("createId") : null);
        obj.setChange(rs.getByte("change"));
        obj.setCalculate(rs.getByte("calculate"));

        // 学生人数：COALESCE 已处理为 0，但保险起见再判 null
        obj.setStudentNum(rs.getInt("studentNum"));
        //课程计分规则
        obj.setAnnounce(rs.getObject("announce") != null ? rs.getInt("announce") : null);

        return obj;
    }
}

