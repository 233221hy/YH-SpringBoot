package cn.xfywz.guozespring.entity.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 额外分数导入数据
 */
@Data
public class ExtraScoreExcelData {

    @ExcelProperty(value = "学号", index = 0)
    @NotBlank(message = "学号不能为空")
    private String studentNumber;

    @ExcelProperty(value = "姓名", index = 1)
    @NotBlank(message = "姓名不能为空")
    private String studentName;

    @ExcelProperty(value = "额外分数", index = 2)
    @NotNull(message = "额外分数不能为空")
    @DecimalMin(value = "0", message = "分数不能小于0")
    @DecimalMax(value = "100", message = "分数不能大于100")
    private BigDecimal extraScore;

}
