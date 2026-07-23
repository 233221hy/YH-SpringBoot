package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
public class WorkReportVO {

    private String chapterName;
    private List<WorkItem> works;

    @Data
    public static class WorkItem {
        private Integer id;
        private String title;
        private Integer totalNum = 0;     // 总份数
        private Integer submitted = 0;    // 已提交
        private Integer unSubmitted = 0;  // 未提交
        private Integer unMarked = 0;     // 待批
        private Integer marked = 0;       // 已批
        private Integer type;             // 作业 / 练习
        private String endTime;           // 格式化时间字符串
    }

    // 静态方法：格式化时间戳
    public static String formatTimestamp(Integer timestamp) {
        if (timestamp == null || timestamp <= 0) return "--";
        LocalDateTime dt = LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.ofHours(8));
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}