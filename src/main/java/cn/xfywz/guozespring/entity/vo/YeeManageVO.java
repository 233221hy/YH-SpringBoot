package cn.xfywz.guozespring.entity.vo;

import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeManageVO {
    private long id;
    private String account;
    private String name;
    private String email;
    private String avatar;
    private String mobile;
    private String gender;
    private String weChat;
    private String intro;
    private long isLock;
    private long collegeId;
    private Long role;
    private Timestamp lastTime;
    private String lastIp;
    private long recommend;
    private long active;
    private String roleName;
    private String collegeName;
    private String colleges;
    private List<String> collegeNames; // 兼职学院名称列表
    private String passport;

    public static YeeManageVO fromResultSet(ResultSet rs) {
        YeeManageVO vo = new YeeManageVO();
        try {
            vo.setId(rs.getLong("id"));
            vo.setAccount(rs.getString("account"));
            vo.setName(rs.getString("name"));
            vo.setEmail(rs.getString("email"));
            vo.setAvatar(rs.getString("avatar"));
            vo.setMobile(rs.getString("mobile"));
            vo.setGender(rs.getString("gender"));
            vo.setWeChat(rs.getString("wechat"));
            vo.setIntro(rs.getString("intro"));
            vo.setIsLock(rs.getLong("isLock"));
            vo.setCollegeId(rs.getLong("collegeId"));
            vo.setRole(rs.getLong("role"));
            vo.setLastTime(rs.getTimestamp("lastTime"));
            vo.setLastIp(rs.getString("lastIp"));
            vo.setRecommend(rs.getLong("recommend"));
            vo.setActive(rs.getLong("active"));
            vo.setCollegeName(rs.getString("collegeName"));
            vo.setColleges(rs.getString("colleges"));
            vo.setPassport(rs.getString("passport"));
        } catch (SQLException e) {
            throw new RuntimeException("映射学校教师列表失败", e);
        }
        return vo;
    }

    /**
     * 解析 colleges 字段
     */
    public static List<Long> parseCollegeIds(String collegesJson) {
        if (collegesJson == null || collegesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // 支持 ["101","102"] 或 [101,102]
            List<String> strList = JSON.parseArray(collegesJson, String.class);
            return strList.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析 colleges 字段失败: {}", collegesJson, e);
            return Collections.emptyList();
        }
    }

}