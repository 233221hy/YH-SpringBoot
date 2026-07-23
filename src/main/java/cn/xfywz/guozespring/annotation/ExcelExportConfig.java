package cn.xfywz.guozespring.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExportConfig {
    /** 导出文件名前缀 */
    String fileName() default "";

    /** sheet名称 */
    String sheetName() default "Sheet1";

    /** 冻结行数 */
    int freezeRows() default 2;

    /** 列宽设置 */
    int[] columnWidths() default {};
}