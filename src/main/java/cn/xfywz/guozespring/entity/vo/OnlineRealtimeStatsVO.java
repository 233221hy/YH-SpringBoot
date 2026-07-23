package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 实时在线统计结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineRealtimeStatsVO {
    private List<Map<String, Object>> series;  // 每个时间桶的在线人数
}
