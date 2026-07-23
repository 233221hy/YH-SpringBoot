package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 根据id查询学生详情
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeStudentDetailVO {
    private Integer id;
    private String number;
    private String name;
    private String avatar;
    private String idCard;
    private String gender;
    private Integer entryYear;
    private Date addDate;
    private String mobile;
    private String weChat;
    private String email;
    private String password;
    private Integer province;
    private Integer city;
    private Integer region;
    private String address;
    private String intro;
    private Integer point;
    private Timestamp addTime;
    private String signature;

    /**
     * 从ResultSet的当前行映射单个VO对象
     */
    public static YeeStudentDetailVO fromResultSet(ResultSet rs) {
        try {
            YeeStudentDetailVO vo = new YeeStudentDetailVO();
            vo.setId(rs.getInt("id"));
            vo.setNumber(rs.getString("number"));
            vo.setName(rs.getString("name"));
            vo.setAvatar(rs.getString("avatar"));
            vo.setIdCard(rs.getString("idCard"));
            vo.setGender(rs.getString("gender"));
            vo.setEntryYear(rs.getInt("entryYear"));
            vo.setAddDate(rs.getDate("addDate"));
            vo.setMobile(rs.getString("mobile"));
            vo.setWeChat(rs.getString("weChat"));
            vo.setEmail(rs.getString("email"));
            vo.setPassword(rs.getString("password"));
            vo.setProvince(rs.getInt("province"));
            vo.setCity(rs.getInt("city"));
            vo.setRegion(rs.getInt("region"));
            vo.setAddress(rs.getString("address"));
            vo.setIntro(rs.getString("intro"));
            vo.setPoint(rs.getInt("point"));
            vo.setAddTime(rs.getTimestamp("addTime"));
            vo.setSignature(rs.getString("signature"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException("映射学生详情失败", e);
        }
    }

}
