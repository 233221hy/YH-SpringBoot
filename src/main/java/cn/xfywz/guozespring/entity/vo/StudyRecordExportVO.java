package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.annotation.ExcelExportConfig;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 学习记录导出类
 */
@Data
@ExcelExportConfig(
        fileName = "学生在线学习记录",
        sheetName = "学习记录",
        // 对应下方字段的顺序设置列宽 (单位：字符宽度)
        columnWidths = {20, 15, 20, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 20}
)
@HeadRowHeight(20)
@ContentRowHeight(18)
@NoArgsConstructor
public class StudyRecordExportVO {

    @ExcelProperty("学号")
    @ColumnWidth(20)
    private String studentNumber;

    @ExcelProperty("学生姓名")
    @ColumnWidth(15)
    private String studentName;

    @ExcelProperty("班级名称")
    @ColumnWidth(20)
    private String className;

    // --- 视频模块 ---
    @ExcelProperty("视频进度%")
    @ColumnWidth(15)
    private String videoPctStr;

    @ExcelProperty("已学视频")
    @ColumnWidth(15)
    private Integer videoLearned;

    @ExcelProperty("需学视频")
    @ColumnWidth(15)
    private Integer videoCount;

    // --- 作业模块 ---
    @ExcelProperty("作业进度%")
    @ColumnWidth(15)
    private String workPctStr;

    @ExcelProperty("已完成作业")
    @ColumnWidth(15)
    private Integer workLearned;

    @ExcelProperty("需完成作业")
    @ColumnWidth(15)
    private Integer workCount;

    // --- 测验模块 ---
    @ExcelProperty("测验进度%")
    @ColumnWidth(15)
    private String examPctStr;

    @ExcelProperty("已完成测验")
    @ColumnWidth(15)
    private Integer examLearned;

    @ExcelProperty("需完成测验")
    @ColumnWidth(15)
    private Integer examCount;

    // --- 讨论模块 ---
    @ExcelProperty("讨论进度%")
    @ColumnWidth(15)
    private String discussPctStr;

    @ExcelProperty("已完成讨论")
    @ColumnWidth(15)
    private Integer discussJoin;

    @ExcelProperty("需完成讨论")
    @ColumnWidth(15)
    private Integer discussCount;

    // --- 学习时长 ---
    @ExcelProperty("学习时长（秒）")
    @ColumnWidth(15)
    private Long studyTimeSeconds;

    @ExcelProperty("学习时长（时分秒）")
    @ColumnWidth(20)
    private String studyTimeFormatted;

    /**
     * 从 ResultSet 构建 VO 并自动计算衍生字段
     */
    public static StudyRecordExportVO fromResultSet(ResultSet rs) {
        try {
            StudyRecordExportVO vo = new StudyRecordExportVO();

            // 1. 基础信息
            vo.setStudentNumber(rs.getString("studentNumber"));
            vo.setStudentName(rs.getString("studentName"));
            vo.setClassName(rs.getString("className"));

            // 2. 视频数据
            int vLearned = rs.getInt("videoLearned");
            int vCount = rs.getInt("videoCount");
            vo.setVideoLearned(vLearned);
            vo.setVideoCount(vCount);
            vo.setVideoPctStr(calcPercentage(vLearned, vCount));

            // 3. 作业数据
            int wLearned = rs.getInt("workLearned");
            int wCount = rs.getInt("workCount");
            vo.setWorkLearned(wLearned);
            vo.setWorkCount(wCount);
            vo.setWorkPctStr(calcPercentage(wLearned, wCount));

            // 4. 测验数据
            int eLearned = rs.getInt("examLearned");
            int eCount = rs.getInt("examCount");
            vo.setExamLearned(eLearned);
            vo.setExamCount(eCount);
            vo.setExamPctStr(calcPercentage(eLearned, eCount));

            // 5. 讨论数据
            int dJoin = rs.getInt("discussJoin");
            int dCount = rs.getInt("discussCount");
            vo.setDiscussJoin(dJoin);
            vo.setDiscussCount(dCount);
            vo.setDiscussPctStr(calcPercentage(dJoin, dCount));

            // 6. 学习时长
            long seconds = rs.getLong("studyTime");
            vo.setStudyTimeSeconds(seconds);
            vo.setStudyTimeFormatted(formatDuration(seconds));

            return vo;
        } catch (SQLException e) {
            throw new RuntimeException("构建学习记录导出VO失败", e);
        }
    }

    /**
     * 计算百分比字符串 (例如: "85%")
     */
    private static String calcPercentage(int learned, int total) {
        if (total <= 0) {
            return "0%";
        }
        // 使用 double 计算并保留整数或一位小数，这里示例保留整数
        double pct = (double) learned / total * 100;
        // 如果需要保留小数可使用 String.format("%.1f%%", pct)
        return String.format("%.0f%%", pct);
    }

    /**
     * 格式化时长为 "x分x秒"
     * 如果超过1小时，可调整为 "x小时x分x秒"，根据需求调整
     */
    private static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0分0秒";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        // 如果希望显示小时：
        // long hours = minutes / 60;
        // long mins = minutes % 60;
        // return String.format("%d小时%d分%d秒", hours, mins, seconds);

        return String.format("%d分%d秒", minutes, seconds);
    }
}