package cn.xfywz.guozespring.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudySession {
    @NotNull @Min(1) private Integer userId;
    @NotNull @Min(1) private Integer nodeId;
    @NotNull @Min(1) private Integer courseId;
    @NotNull @Min(1) private Integer schoolId;
    @NotNull private Long startTime;
    @NotNull private Long lastActive;
    private String ip = "";
    private String terminal = "web";
    private String startProgress = "0";
    private String currentProgress = "0";

}
