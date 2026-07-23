package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
public class YeeManageListVO {
    //ym.id, ym.account, ym.name, ym.lastIp, ym.lastTime, ym.isLock, ym.email,
    //        ym.avatar, ym.role, ym.mobile, ym.collegeId, ym.active, yc.name AS collegeName
    private long id;
    private String account;
    private String name;
    private String avatar;
    private String mobile;
    private long isLock;
    private long collegeId;
    private Long role;
    private Timestamp lastTime;
    private String lastIp;
    private long active;
    private String collegeName;
    private String roleName;

    public static YeeManageListVO fromResultSet(ResultSet rs) {
        YeeManageListVO vo = new YeeManageListVO();
        try {
            vo.setId(rs.getLong("id"));
            vo.setAccount(rs.getString("account"));
            vo.setName(rs.getString("name"));
            vo.setAvatar(rs.getString("avatar"));
            vo.setMobile(rs.getString("mobile"));
            vo.setIsLock(rs.getLong("isLock"));
            vo.setCollegeId(rs.getLong("collegeId"));
            vo.setRole(rs.getLong("role"));
            vo.setLastTime(rs.getTimestamp("lastTime"));
            vo.setLastIp(rs.getString("lastIp"));
            vo.setActive(rs.getLong("active"));
            vo.setCollegeName(rs.getString("collegeName"));
        } catch (Exception e) {
            throw new RuntimeException("映射学校教师列表失败", e);
        }
        return vo;
    }
}
