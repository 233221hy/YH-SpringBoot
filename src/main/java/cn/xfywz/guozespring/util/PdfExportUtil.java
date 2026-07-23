package cn.xfywz.guozespring.util;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public final class PdfExportUtil {

    private PdfExportUtil() {}

    public static BaseFont chineseFont() throws Exception {
        return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
    }

    public static Document initDocument(OutputStream outputStream) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, outputStream);
        document.open();
        return document;
    }

    public static void addTitle(Document document, String title) throws Exception {
        BaseFont bf = chineseFont();
        Font titleFont = new Font(bf, 18, Font.BOLD);
        Paragraph paragraph = new Paragraph(title, titleFont);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(20);
        document.add(paragraph);
    }

    public static void addInfoTable(Document document, Map<String, String> infoMap) throws Exception {
        BaseFont bf = chineseFont();
        Font keyFont = new Font(bf, 10);
        Font valueFont = new Font(bf, 10);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            PdfPCell keyCell = new PdfPCell(new Phrase(entry.getKey(), keyFont));
            keyCell.setBackgroundColor(new BaseColor(245, 245, 245));
            keyCell.setPadding(5);
            table.addCell(keyCell);

            PdfPCell valueCell = new PdfPCell(new Phrase(entry.getValue() != null ? entry.getValue() : "", valueFont));
            valueCell.setPadding(5);
            table.addCell(valueCell);
        }
        document.add(table);
    }

    public static void addContent(Document document, String htmlContent) throws Exception {
        BaseFont bf = chineseFont();
        Font contentFont = new Font(bf, 11);

        String plainText = removeHtmlTags(htmlContent);
        Paragraph paragraph = new Paragraph(plainText, contentFont);
        paragraph.setSpacingAfter(10);
        paragraph.setLeading(20);
        document.add(paragraph);
    }

    public static void addAttachments(Document document, List<String> urls) throws Exception {
        BaseFont bf = chineseFont();
        Font linkFont = new Font(bf, 10, Font.UNDERLINE, BaseColor.BLUE);

        Paragraph header = new Paragraph("附件列表：", new Font(bf, 10, Font.BOLD));
        header.setSpacingAfter(5);
        document.add(header);

        for (int i = 0; i < urls.size(); i++) {
            Paragraph link = new Paragraph((i + 1) + ". " + urls.get(i), linkFont);
            link.setSpacingAfter(3);
            document.add(link);
        }
    }

    public static void closeDocument(Document document) {
        if (document != null && document.isOpen()) {
            document.close();
        }
    }

    private static String removeHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "")
                   .replace("&nbsp;", " ")
                   .replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
}