package cn.xfywz.guozespring.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 学习会话结束请求参数
 */
@Data
public class StudySessionEndDTO {
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
}