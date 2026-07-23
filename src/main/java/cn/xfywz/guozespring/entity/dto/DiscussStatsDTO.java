package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscussStatsDTO {
    private Integer pageNum;
    private Integer pageSize;
    private long schoolId;
    private long courseId;
    private long classId;
    private String keyword;
    // 讨论主题Id（下拉：全部=0/不传；否则为具体 discussId）
    private long discussId;
    // 视频讨论：章节（节点）Id，全部=0/不传
    private long nodeId;
    // 发帖时间范围（毫秒时间戳），用于过滤 yee_discuss_reply.addTime / yee_node_discuss.addTime
    private Long startTime;
    private Long endTime;
    // 统计对象类型：1-学生（默认），2-老师（对应“老师讨论统计”页签）
    private Integer statsType;
    // 详情目标用户（学生或老师）的ID
    private long userId;
    // 列表类型：1-发表（帖子），2-回复
    private Integer listType;
}
