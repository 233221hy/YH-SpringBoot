package cn.xfywz.guozespring.excel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.handler.*;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import org.apache.poi.ss.usermodel.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Excel导出样式工具类
 */
public final class ExcelExportStyles {

    // 样式常量
    private static final float TITLE_ROW_HEIGHT_POINTS = 40f;   // 标题行行高（磅）
    private static final short TITLE_FONT_SIZE = 18;           // 标题行字号
    private static final short HEAD_FONT_SIZE = 12;            // 表头行字号
    private static final short CONTENT_FONT_SIZE = 11;         // 内容行字号
    private static final String FONT_NAME = "宋体";             // 字体名称
    private static final int AUTO_WIDTH_PADDING = 512;         // 自适应列宽增加的内边距（单位：1/256字符宽度）
    private static final int MAX_COLUMN_WIDTH = 255;           // Excel 最大列宽（字符数）

    private ExcelExportStyles() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 创建默认的表头/内容样式策略
     * <p>表头样式：居中、细边框、加粗、12号宋体。</p>
     * <p>内容样式：居中、细边框、常规、11号宋体。</p>
     *
     * @return 水平单元格样式策略对象，可直接注册到 EasyExcel 写入器中
     */
    public static HorizontalCellStyleStrategy defaultStyleStrategy() {
        // 表头样式
        WriteCellStyle headStyle = createBaseStyle();
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headFont.setFontHeightInPoints(HEAD_FONT_SIZE);
        headFont.setFontName(FONT_NAME);
        headStyle.setWriteFont(headFont);
        // 内容样式
        WriteCellStyle contentStyle = createBaseStyle();
        WriteFont contentFont = new WriteFont();
        contentFont.setFontName(FONT_NAME);
        contentFont.setFontHeightInPoints(CONTENT_FONT_SIZE);
        contentFont.setBold(false);
        contentStyle.setWriteFont(contentFont);

        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    /**
     * 创建基础单元格样式（居中、细边框）
     *
     * @return 基础单元格样式（未设置字体）
     */
    private static WriteCellStyle createBaseStyle() {
        WriteCellStyle style = new WriteCellStyle();
        style.setHorizontalAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }


    /**
     * 默认标题行样式处理器
     * <p>第0行作为标题行，设置行高为40磅，字体为18号加粗宋体，所有单元格水平垂直居中。</p>
     *
     * @param columnCount 标题行所占的列数（必须与数据列数一致）
     * @return 行写入处理器，用于设置标题行样式
     */
    public static WriteHandler defaultTitleRow(int columnCount) {
//        if (columnCount <= 0) {
//            return (RowWriteHandler) (writeSheetHolder, writeTableHolder, row, relativeRowIndex, isHead) -> {};
//        }
        return new RowWriteHandler() {
            @Override
            public void afterRowDispose(WriteSheetHolder writeSheetHolder,
                                        WriteTableHolder writeTableHolder,
                                        Row row,
                                        Integer relativeRowIndex,
                                        Boolean isHead) {
                if (row == null || row.getRowNum() != 0) return;

                row.setHeightInPoints(TITLE_ROW_HEIGHT_POINTS);
                Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
                Font font = workbook.createFont();
                font.setBold(true);
                font.setFontHeightInPoints(TITLE_FONT_SIZE);
                font.setFontName(FONT_NAME);

                for (int i = 0; i < columnCount; i++) {
                    Cell cell = row.getCell(i);
                    if (cell == null) {
                        cell = row.createCell(i);
                    }
                    CellStyle style = workbook.createCellStyle();
                    style.setFont(font);
                    style.setAlignment(HorizontalAlignment.CENTER);
                    style.setVerticalAlignment(VerticalAlignment.CENTER);
                    cell.setCellStyle(style);
                }
            }
        };
    }

    /**
     * 冻结窗格和列宽处理器
     * <p>在创建 sheet 后设置冻结窗格（按行冻结）和各列宽度。</p>
     *
     * @param columnWidths 各列宽度（字符数），例如 {15, 20, 10}，内部会自动转换为 Excel 宽度单位（1字符=256单位）
     * @param freezeRows   冻结的行数（从第1行开始计算，0表示不冻结）
     * @return sheet 写入处理器，用于设置冻结和列宽
     */
    public static WriteHandler freezeAndWidth(int[] columnWidths, int freezeRows) {
        return new SheetWriteHandler() {
            @Override
            public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder,
                                         WriteSheetHolder writeSheetHolder) {
                Sheet sheet = writeSheetHolder.getSheet();
                if (freezeRows > 0) {
                    sheet.createFreezePane(0, freezeRows, 0, freezeRows);
                }
                if (columnWidths != null && columnWidths.length > 0) {
                    for (int i = 0; i < columnWidths.length; i++) {
                        int width = Math.min(columnWidths[i], MAX_COLUMN_WIDTH) * 256;
                        sheet.setColumnWidth(i, width);
                    }
                }
            }
        };
    }

    /**
     * 文本列处理器（基于实体类自动识别）
     * <p>通过 {@link ExcelDataPreprocessor#identifyTextColumns(Class)} 识别哪些列需要设置为文本格式，
     * 避免长数字串（如身份证号、手机号）被 Excel 自动转为科学计数法。</p>
     *
     * @param clazz 导出数据对应的实体类（需包含 {@link com.alibaba.excel.annotation.ExcelProperty} 注解）
     * @return 单元格写入处理器，将目标列格式设为文本
     */
    public static WriteHandler textColumns(Class<?> clazz) {
        return createTextColumnHandler(ExcelDataPreprocessor.identifyTextColumns(clazz));
    }

    /**
     * 文本列处理器（基于列索引数组）
     * <p>手动指定需要设置为文本格式的列索引（从0开始）。</p>
     *
     * @param columnIndexes 需要文本格式的列索引数组，例如 {@code {0, 3, 5}}
     * @return 单元格写入处理器，将指定列格式设为文本
     */
    public static WriteHandler textColumns(int[] columnIndexes) {
        Set<Integer> set = new HashSet<>();
        for (int col : columnIndexes) {
            set.add(col);
        }
        return createTextColumnHandler(set);
    }

    /**
     * 创建通用的文本列处理器
     *
     * @param textColumns 需要设为文本格式的列索引集合
     * @return 单元格写入处理器
     */
    private static WriteHandler createTextColumnHandler(Set<Integer> textColumns) {
        return new CellWriteHandler() {
            @Override
            public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                                         WriteTableHolder writeTableHolder,
                                         List<WriteCellData<?>> cellDataList,
                                         Cell cell,
                                         Head head,
                                         Integer relativeRowIndex,
                                         Boolean isHead) {
                if (cell == null || Boolean.TRUE.equals(isHead)) return;
                if (!textColumns.contains(cell.getColumnIndex())) return;

                Workbook workbook = cell.getSheet().getWorkbook();
                CellStyle cellStyle = workbook.createCellStyle();
                CellStyle originalStyle = cell.getCellStyle();
                if (originalStyle != null) {
                    cellStyle.cloneStyleFrom(originalStyle);
                }
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
                cell.setCellStyle(cellStyle);
            }
        };
    }

    /**
     * 自适应列宽处理器
     * <p>调用 POI 的 {@link Sheet#autoSizeColumn(int)} 自动调整列宽，并额外增加 {@value #AUTO_WIDTH_PADDING} 单位的内边距。</p>
     * <p><strong>注意：</strong>对大量数据（超过数千行）时性能较差，建议手动指定列宽或仅用于小数据量导出。</p>
     *
     * @param columnCount 需要调整列宽的列数（应与数据列数一致）
     * @return sheet 写入处理器，在创建 sheet 后自动调整列宽
     */
    public static WriteHandler autoColumnWidth(int columnCount) {
//        if (columnCount <= 0) {
//            return (SheetWriteHandler) (writeWorkbookHolder, writeSheetHolder) -> {};
//        }
        return new SheetWriteHandler() {
            @Override
            public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder,
                                         WriteSheetHolder writeSheetHolder) {
                Sheet sheet = writeSheetHolder.getSheet();
                for (int i = 0; i < columnCount; i++) {
                    sheet.autoSizeColumn(i);
                    int currentWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, currentWidth + AUTO_WIDTH_PADDING);
                }
            }
        };
    }
}