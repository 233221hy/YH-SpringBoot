package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Data
public class ManageListVO {
//    ym.id, ym.account, ym.name, ym.lastIp, ym.lastTime, ym.isLock, ym.email,
//    ym.avatar, ym.role, ym.mobile, ym.active, ym.schoolId, ym.email
    private Integer id;
    private String account;
    private String name;
    private String lastIp;
    private Timestamp lastTime;
    private Integer isLock;
    private String email;
    private String avatar;
    private Long role;
    private String mobile;
    private Integer active;
    private Integer schoolId;

    // 映射
    public static ManageListVO fromResultSet(ResultSet rs){
        ManageListVO vo = new ManageListVO();
        try {
            vo.setId(rs.getInt("id"));
            vo.setAccount(rs.getString("account"));
            vo.setName(rs.getString("name"));
            vo.setLastIp(rs.getString("lastIp"));
            vo.setLastTime(rs.getTimestamp("lastTime"));
            vo.setIsLock(rs.getInt("isLock"));
            vo.setEmail(rs.getString("email"));
            vo.setAvatar(rs.getString("avatar"));
            vo.setRole(rs.getLong("role"));
            vo.setMobile(rs.getString("mobile"));
            vo.setActive(rs.getInt("active"));
            vo.setSchoolId(rs.getInt("schoolId"));
        } catch (SQLException e) {
            throw new RuntimeException("映射学校教师列表失败", e);
        }
        return vo;
    }
}
