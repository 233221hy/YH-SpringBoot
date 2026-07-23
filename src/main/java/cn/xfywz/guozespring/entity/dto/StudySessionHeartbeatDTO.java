package cn.xfywz.guozespring.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 学习会话心跳请求参数
 */
@Data
public class StudySessionHeartbeatDTO {
    @NotBlank(message = "sessionId 不能为空") private String sessionId;
    @NotBlank(message = "进度不能为空") private String progress;
}