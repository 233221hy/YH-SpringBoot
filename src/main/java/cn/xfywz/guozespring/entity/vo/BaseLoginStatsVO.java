package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * 登录统计基础VO，包含公共字段
 */
@Data
public abstract class BaseLoginStatsVO {
    protected Integer loginCount;
    protected Long totalDurationSeconds;
    protected String totalDurationFormatted;
    protected Timestamp lastLoginTime;
    protected Long lastSessionDuration;
    protected String lastSessionDurationFormatted;
    protected String lastLoginIP;
    protected String lastLoginPlatform;
    protected Integer isOnline;

    // 从ResultSet中提取公共字段
    protected void populateBaseFields(ResultSet rs) throws SQLException {
        this.loginCount = rs.getInt("loginCount");
        this.totalDurationSeconds = rs.getLong("totalDurationSeconds");
        this.totalDurationFormatted = rs.getString("totalDurationFormatted");
        this.lastLoginTime = rs.getTimestamp("lastLoginTime");

        Object lastSessionDurationObj = rs.getObject("lastSessionDuration");
        this.lastSessionDuration = lastSessionDurationObj == null ? null : rs.getLong("lastSessionDuration");
        this.lastSessionDurationFormatted = rs.getString("lastSessionDurationFormatted");
        this.lastLoginIP = rs.getString("lastLoginIP");
        this.lastLoginPlatform = rs.getString("lastLoginPlatform");
        this.isOnline = rs.getInt("isOnline");
    }
}
