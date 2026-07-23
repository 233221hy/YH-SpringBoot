package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.xfywz.guozespring.entity.vo.PracticeReportVO;
import cn.xfywz.guozespring.service.student.YeePracticeReportService;
import cn.xfywz.guozespring.util.CosClientUtil;
import cn.xfywz.guozespring.util.PdfExportUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("stuYeePracticeReportServiceImpl")
public class YeePracticeReportServiceImpl implements YeePracticeReportService {

    private static final Logger log = LoggerFactory.getLogger(YeePracticeReportServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private DatabaseUtil databaseUtil;

    @Override
    public Result myReport(int schoolId, int courseId, int studentId) throws Exception {
        String sql = """
            SELECT pr.*, s.number, s.name, cc.name AS className
            FROM yee_practice_report pr
            LEFT JOIN yee_student s ON pr.studentId = s.id
            LEFT JOIN yee_course_class cc ON pr.classId = cc.id
            WHERE pr.courseId = ? AND pr.studentId = ?
            """;

        PracticeReportVO vo = databaseUtil.query(schoolId)
                .sql(sql)
                .params(courseId, studentId)
                .single(rs -> {
                    PracticeReportVO v = new PracticeReportVO();
                    v.setId(rs.getLong("id"));
                    v.setStudentNumber(rs.getString("number"));
                    v.setStudentName(rs.getString("name"));
                    v.setClassName(rs.getString("className"));
                    v.setTitle(rs.getString("title"));
                    v.setSubmitTime(rs.getTimestamp("submitTime"));
                    v.setStatus(rs.getInt("status"));
                    v.setContent(rs.getString("content"));
                    v.setFiles(rs.getString("files"));
                    v.setRemark(rs.getString("remark"));
                    v.setReviewTime(rs.getTimestamp("reviewTime"));
                    v.setPdfPath(rs.getString("pdfPath"));
                    return v;
                })
                .orElse(null);

        if (vo != null && vo.getStatus() != null && vo.getStatus() == 2
                && (vo.getPdfPath() == null || vo.getPdfPath().isEmpty())) {
            String pdfUrl = generateAndUploadPdf(vo);
            if (pdfUrl != null) {
                databaseUtil.executeUpdate(schoolId,
                        "UPDATE yee_practice_report SET pdfPath = ? WHERE id = ?",
                        pdfUrl, vo.getId());
                vo.setPdfPath(pdfUrl);
            }
        }

        return Result.success(vo);
    }

    private String generateAndUploadPdf(PracticeReportVO vo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = PdfExportUtil.initDocument(baos);
            try {
                PdfExportUtil.addTitle(document,
                        vo.getTitle() != null ? vo.getTitle() : "实践报告");

                String submitTimeStr = vo.getSubmitTime() != null ?
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(vo.getSubmitTime()) : "";

                Map<String, String> infoMap = new LinkedHashMap<>();
                infoMap.put("姓名", vo.getStudentName() != null ? vo.getStudentName() : "");
                infoMap.put("学号", vo.getStudentNumber() != null ? vo.getStudentNumber() : "");
                infoMap.put("班级", vo.getClassName() != null ? vo.getClassName() : "");
                infoMap.put("提交时间", submitTimeStr);
                infoMap.put("通过状态", "已通过");
                PdfExportUtil.addInfoTable(document, infoMap);

                PdfExportUtil.addContent(document,
                        vo.getContent() != null ? vo.getContent() : "");

                List<String> urls = parseFileUrls(vo.getFiles());
                if (urls != null && !urls.isEmpty()) {
                    PdfExportUtil.addAttachments(document, urls);
                }
            } finally {
                PdfExportUtil.closeDocument(document);
            }

            byte[] pdfBytes = baos.toByteArray();
            return CosClientUtil.uploadBytes(pdfBytes,
                    (vo.getStudentNumber() != null ? vo.getStudentNumber() : "report") + "_实践报告.pdf");
        } catch (Exception e) {
            log.warn("生成实践报告PDF失败：studentId={}, reportId={}", vo.getStudentNumber(), vo.getId(), e);
            return null;
        }
    }

    private List<String> parseFileUrls(String filesJson) {
        if (filesJson == null || filesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(filesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("解析附件JSON失败：{}", filesJson, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Result submit(int schoolId, int courseId, int studentId,
                         String title, String content, String files) throws Exception {

        if (title == null || title.isBlank()) {
            return Result.error("报告标题不能为空");
        }
        if (title.length() > 30) {
            return Result.error("报告标题不超过30字");
        }

        // Validate: student is enrolled in a practice course within time range
        String checkSql = """
            SELECT cs.id, c.startDate, c.endDate, c.isPractice
            FROM yee_course_student cs
            JOIN yee_course c ON cs.courseId = c.id
            WHERE cs.courseId = ? AND cs.studentId = ?
            """;

        var checkResult = databaseUtil.query(schoolId)
                .sql(checkSql)
                .params(courseId, studentId)
                .single(rs -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("startDate", rs.getDate("startDate"));
                    m.put("endDate", rs.getDate("endDate"));
                    m.put("isPractice", rs.getInt("isPractice"));
                    return m;
                })
                .orElse(null);

        if (checkResult == null) {
            return Result.error("您未选修该课程");
        }

        int isPractice = (int) checkResult.get("isPractice");
        if (isPractice != 1) {
            return Result.error("该课程不是实践课程");
        }

        Date now = new Date();
        Date startDate = (Date) checkResult.get("startDate");
        Date endDate = (Date) checkResult.get("endDate");

        if (startDate != null && now.before(startDate)) {
            return Result.error("课程尚未开始");
        }
        if (endDate != null && now.after(endDate)) {
            return Result.error("课程已结束");
        }

        // Get classId from enrollment
        String classSql = "SELECT classId FROM yee_course_student WHERE courseId = ? AND studentId = ?";
        Long classId = databaseUtil.query(schoolId)
                .sql(classSql)
                .params(courseId, studentId)
                .scalar(rs -> rs.getLong("classId"))
                .orElse(null);

        // Check existing report
        String existSql = "SELECT id, status FROM yee_practice_report WHERE courseId = ? AND studentId = ?";
        var existing = databaseUtil.query(schoolId)
                .sql(existSql)
                .params(courseId, studentId)
                .single(rs -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("status", rs.getInt("status"));
                    return m;
                })
                .orElse(null);

        if (existing != null) {
            int currentStatus = (int) existing.get("status");
            if (currentStatus == 1) {
                return Result.error("报告待审核中，不可重复提交");
            }
            // status=2 (passed) or status=3 (rejected) -> allow resubmit
            Long reportId = (Long) existing.get("id");
            String updateSql = """
                UPDATE yee_practice_report
                SET title = ?, content = ?, files = ?, status = 1,
                    submitTime = ?, reviewTime = NULL, reviewerId = NULL, remark = NULL, pdfPath = NULL
                WHERE id = ?
                """;
            databaseUtil.executeUpdate(schoolId, updateSql,
                    title, content, files != null ? files : "[]",
                    new Timestamp(System.currentTimeMillis()), reportId);
        } else {
            String insertSql = """
                INSERT INTO yee_practice_report
                (courseId, classId, studentId, title, content, files, status, submitTime, schoolId, addTime)
                VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                """;
            databaseUtil.executeUpdate(schoolId, insertSql,
                    courseId, classId, studentId, title, content,
                    files != null ? files : "[]",
                    new Timestamp(System.currentTimeMillis()),
                    schoolId,
                    new Timestamp(System.currentTimeMillis()));
        }

        return Result.success("提交成功");
    }
}