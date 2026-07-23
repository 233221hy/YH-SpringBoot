package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentOnlineStatsVO {
    private Long id;                     // 学生ID
    private String stuNumber;            // 学号
    private String stuName;              // 姓名
    private Long classId;                // 班级ID
    private String className;            // 班级名称
    private Long loginCount;             // 登录次数
    private Long totalDurationSeconds;   // 总时长（秒）
    private String totalDurationFormatted; // 总时长格式化字符串
    private Timestamp lastLoginTime;     // 最近一次登录时间
    private Long lastSessionDuration;    // 最近一次会话时长（秒）
    private String lastSessionDurationFormatted; // 最近一次会话时长格式化
    private String lastLoginIP;          // 最近登录IP
    private String lastLoginPlatform;    // 最近登录平台
    private Integer isOnline;            // 是否在线（1在线，0离线）
}
