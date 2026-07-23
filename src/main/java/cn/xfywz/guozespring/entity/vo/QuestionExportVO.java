package cn.xfywz.guozespring.entity.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试题导出VO类
 */
@Data
@ContentRowHeight(20)
@HeadRowHeight(25)
@ColumnWidth(20)
public class QuestionExportVO {

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("题目内容")
    @ColumnWidth(30)
    private String topic;

    @ExcelProperty("难易程度")
    private String levelName;

    @ExcelProperty("类型")
    private String typeName;

    @ExcelProperty("默认分值")
    private Integer score;

    @ExcelProperty("选项A")
    private String optionA;

    @ExcelProperty("选项B")
    private String optionB;

    @ExcelProperty("选项C")
    private String optionC;

    @ExcelProperty("选项D")
    private String optionD;

    @ExcelProperty("选项E")
    private String optionE;

    @ExcelProperty("选项F")
    private String optionF;

    @ExcelProperty("漏选计分模式")
    private String scoreModeName;

    @ExcelProperty("得分比A")
    private Double scoreRatioA;

    @ExcelProperty("得分比B")
    private Double scoreRatioB;

    @ExcelProperty("得分比C")
    private Double scoreRatioC;

    @ExcelProperty("得分比D")
    private Double scoreRatioD;

    @ExcelProperty("得分比E")
    private Double scoreRatioE;

    @ExcelProperty("得分比F")
    private Double scoreRatioF;

    @ExcelProperty("漏选1项")
    private String missScore1;

    @ExcelProperty("漏选2项")
    private String missScore2;

    @ExcelProperty("漏选3项")
    private String missScore3;

    @ExcelProperty("漏选4项")
    private String missScore4;

    @ExcelProperty("漏选5项")
    private String missScore5;

    @ExcelProperty("题目解析")
    @ColumnWidth(30)
    private String analysis;

    // 不写入Excel
    @ExcelIgnore
    private Integer type;
    @ExcelIgnore
    private Integer level;
    @ExcelIgnore
    private Integer scoreMode;
    @ExcelIgnore
    private String upload;
    @ExcelIgnore
    private LocalDateTime addTime;
}