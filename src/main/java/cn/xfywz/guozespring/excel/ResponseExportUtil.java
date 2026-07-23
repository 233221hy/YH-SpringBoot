package cn.xfywz.guozespring.excel;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 通用导出响应头工具
 */
public final class ResponseExportUtil {
    private ResponseExportUtil() {}

    /**
     * 设置 Excel(xlsx) 下载响应头
     */
    public static void setExcelRespProp(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        try {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition",
                "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        } catch (Exception e) {
            // 退化处理
            response.setHeader("Content-disposition",
                "attachment;filename=" + fileName + ".xlsx");
        }
    }
}