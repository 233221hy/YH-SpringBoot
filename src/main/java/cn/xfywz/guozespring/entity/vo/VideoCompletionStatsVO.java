package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 视频完成分布统计（按数量、按时长）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCompletionStatsVO {
    private Map<String, Long> byCount;      // 按数量完成比例分布
    private Map<String, Long> byDuration;   // 按时长完成比例分布
}
