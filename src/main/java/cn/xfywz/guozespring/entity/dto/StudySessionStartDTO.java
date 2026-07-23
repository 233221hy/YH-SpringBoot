package cn.xfywz.guozespring.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学习会话开始请求参数
 */
@Data
public class StudySessionStartDTO {
    @NotNull(message = "学校ID不能为空") private Integer schoolId;
    @NotNull(message = "用户ID不能为空") private Integer userId;
    @NotNull(message = "节点ID不能为空") private Integer nodeId;
    @NotNull(message = "课程ID不能为空") private Integer courseId;
    private String terminal;
}