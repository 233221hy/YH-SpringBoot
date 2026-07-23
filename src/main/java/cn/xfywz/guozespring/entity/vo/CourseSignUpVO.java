package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CourseSignUpVO {
    //课程id
    private Integer Id;
    //课程名
    private String courseName;
    //课程封面
    private String cover;
    //报名状态
    private String signStatus;
    //课程类型
    private Integer mode;
//    //学习进度
//    private Integer progress;
    //开课开始时间
    private String startDate;
    //开课结束时间
    private String endDate;
    //课程报名开始时间
    private String signStartTime;
    //课程报名结束时间
    private String signEndTime;
    //课程学分
    private Double credit;
    //主讲老师
    private String lecturerName;
    //报名id
    private Integer signId;
    //学习人数
    private  Integer stuCount;

    /**
     * 将ResultSet映射为导出VO列表
     */
    public static List<CourseSignUpVO> mapCourseSignUpVO(ResultSet rs) throws SQLException {
        List<CourseSignUpVO> list = new ArrayList<>();
        while (rs.next()) {
            CourseSignUpVO vo = new CourseSignUpVO();
            vo.setId(rs.getInt("Id"));
            vo.setCourseName(rs.getString("courseName"));
            vo.setCover(rs.getString("cover"));
            String signStatus = null;
            try {
                signStatus = rs.getString("signStatus");
            } catch (SQLException ignored) {}
            vo.setSignStatus(signStatus);
            vo.setMode(rs.getInt("mode"));
//            vo.setProgress(rs.getInt("progress"));
            vo.setStartDate(rs.getString("startDate"));
            vo.setEndDate(rs.getString("endDate"));
            vo.setSignStartTime(rs.getString("signStartTime"));
            vo.setSignEndTime(rs.getString("signEndTime"));
            vo.setCredit(rs.getDouble("credit"));
            vo.setLecturerName(rs.getString("lecturerName"));
            Integer signId = null;
            try {
                signId = rs.getInt("signId");
            } catch (SQLException ignored) {}
            vo.setSignId(signId);
            int stuCount = 0;
            try {
                stuCount = rs.getInt("stuCount");
            } catch (SQLException ignored) {}
            vo.setStuCount(stuCount);
            list.add(vo);
        }
        return list;
    }

    public static CourseSignUpVO fromResultSet(ResultSet rs){
        try {
            CourseSignUpVO vo = new CourseSignUpVO();
            vo.setId(rs.getInt("id"));
            vo.setCourseName(rs.getString("courseName"));
            vo.setCover(rs.getString("cover"));
            vo.setSignStatus(rs.getString("signStatus"));
            vo.setMode(rs.getInt("mode"));
            vo.setStartDate(rs.getString("startDate"));
            vo.setEndDate(rs.getString("endDate"));
            vo.setSignStartTime(rs.getString("signStartTime"));
            vo.setSignEndTime(rs.getString("signEndTime"));
            vo.setCredit(rs.getDouble("credit"));
            vo.setLecturerName(rs.getString("lecturerName"));
            vo.setSignId(rs.getInt("signId"));   // signId 可能为 0（joined/available 时设为 0）
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
