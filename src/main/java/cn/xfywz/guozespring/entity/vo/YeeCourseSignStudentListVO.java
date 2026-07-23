package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程报名列表回显数据
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseSignStudentListVO {

    private Integer id;
    private Integer studentId;
    private String studentNumber;
    private String studentName;
    private String idCard;
    private String gender;
    private String collegeName;
    private String className;
    private Integer states;
    private Timestamp signTime;
    private Integer courseClassId;

//    public static List<YeeCourseSignStudentListVO> fromResultSet(ResultSet rs) throws SQLException {
//        List<YeeCourseSignStudentListVO> list = new ArrayList<>();
//        while (rs.next()) {
//            YeeCourseSignStudentListVO vo = new YeeCourseSignStudentListVO();
//            vo.setId(rs.getInt("id"));
//            vo.setStudentId(rs.getInt("studentId"));
//            vo.setStudentNumber(rs.getString("studentNumber"));
//            vo.setStudentName(rs.getString("studentName"));
//            String idCard = rs.getString("idCard");
//            vo.setIdCard(maskIdCard(idCard));
//            vo.setGender(rs.getString("gender"));
//            vo.setCollegeName(rs.getString("collegeName"));
//            vo.setClassName(rs.getString("className"));
//            vo.setStates(rs.getInt("states"));
//            vo.setSignTime(rs.getTimestamp("signTime"));
//            vo.setCourseClassId(rs.getInt("courseClassId"));
//            list.add(vo);
//        }
//        return list;
//    }

    public static YeeCourseSignStudentListVO fromResultSet(ResultSet rs) {
        try {
            YeeCourseSignStudentListVO vo = new YeeCourseSignStudentListVO();
            vo.setId(rs.getInt("id"));
            vo.setStudentId(rs.getInt("studentId"));
            vo.setStudentNumber(rs.getString("studentNumber"));
            vo.setStudentName(rs.getString("studentName"));
            String idCard = rs.getString("idCard");
            vo.setIdCard(maskIdCard(idCard));
            vo.setGender(rs.getString("gender"));
            vo.setCollegeName(rs.getString("collegeName"));
            vo.setClassName(rs.getString("className"));
            vo.setStates(rs.getInt("states"));
            vo.setSignTime(rs.getTimestamp("signTime"));
            vo.setCourseClassId(rs.getInt("courseClassId"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String maskIdCard(String idCard) {
        if (idCard == null) return "";
        int len = idCard.length();
        if (len <= 12) {
            return idCard.substring(0, Math.min(6, len)) + "******" + (len > 12 ? idCard.substring(12) : "");
        }
        return idCard.substring(0, 6) + "******" + idCard.substring(12);
    }


}
