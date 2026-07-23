package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.entity.vo.QuestionExportVO;
import cn.xfywz.guozespring.excel.ExcelExportStyles;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.QuestionExportService;
import cn.xfywz.guozespring.service.teacher.YeeQuestionService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 试题导出服务类
 */
@Service
public class QuestionExportServiceImpl implements QuestionExportService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionExportServiceImpl.class);

    @Autowired
    private YeeQuestionService yeeQuestionService;

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    /**
     * 导出试题数据为Excel
     * @param response HTTP响应对象
     * @param schoolId 学校ID
     * @param topic 题干搜索关键词
     * @param createId 创建人ID
     * @param type 试题类型
     * @param level 难度等级
     * @param cateBid 大类ID
     * @param cateMid 中类ID
     */
    @Override
    public void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, 
                              Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid) throws Exception {
        OutputStream outputStream = null;
        ExcelWriter excelWriter = null;
        try {
            logger.info("开始导出试题数据: schoolId={}, topic={}, createId={}, type={}, level={}, cateBid={}, cateMid={}", 
                       schoolId, topic, createId, type, level, cateBid, cateMid);
            
            // 1. 设置响应头
            LocalDateTime now = LocalDateTime.now();
            String fileName = "试题题目导出_" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            
            logger.info("设置响应头完成，文件名: {}", fileName);
            
            // 2. 获取输出流
            outputStream = response.getOutputStream();

            logger.info("获取输出流成功，准备创建ExcelWriter");

            // 3. 创建ExcelWriter并写入数据（流式写入）
            ExcelWriterBuilder writerBuilder = EasyExcel.write(outputStream, QuestionExportVO.class)
                    .registerWriteHandler(ExcelExportStyles.defaultStyleStrategy());
            excelWriter = writerBuilder.build();
            WriteSheet writeSheet = EasyExcel.writerSheet("试题数据").build();
            
            // 4. 分页查询并流式写入数据
            int pageSize = 10000; // 每页10000条数据
            int pageNum = 1;
            long totalExported = 0;

            // 获取学校信息和数据库连接
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            if (connection == null) {
                throw new Exception("无法获取数据库连接");
            }



            
            try {
                while (true) {
                    // 测试执行时间
                    long startTime = System.currentTimeMillis();
                    // 获取分页数据，复用数据库连接
                    Object result = ((YeeQuestionServiceImpl) yeeQuestionService).exportAllWithPagination(
                        connection, schoolId, pageSize, pageNum, topic, createId, type, level, cateBid, cateMid);
                
                    // 提取试题列表
                    List<YeeQuestion> questions = new ArrayList<>();
                    if (result instanceof Result) {
                        Object data = ((Result) result).getData();
                        if (data instanceof List) {
                            questions = (List<YeeQuestion>) data;
                        }
                    } else if (result instanceof Map && ((Map<?, ?>) result).containsKey("data")) {
                        Object data = ((Map<?, ?>) result).get("data");
                        if (data instanceof List) {
                            questions = (List<YeeQuestion>) data;
                        }
                    }

                    // 如果没有数据了，跳出循环
                    if (questions.isEmpty()) {
                        break;
                    }

                    logger.info("获取到第{}页{}条试题数据，开始转换为导出VO对象", pageNum, questions.size());

                    // 转换为导出VO对象
                    List<QuestionExportVO> exportList = convertToExportVO(questions);

                    logger.info("开始写入第{}页{}条数据到Excel", pageNum, exportList.size());

                    // 写入数据到Excel（流式写入）
                    excelWriter.write(exportList, writeSheet);

                    // 测试执行时间
                    long endTime = System.currentTimeMillis();
                    logger.info("执行时间：{}ms", endTime - startTime);

                    totalExported += exportList.size();

                    // 如果当前页数据少于pageSize，说明已经是最后一页，跳出循环
                    if (questions.size() < pageSize) {
                        break;
                    }

                    // 继续下一页
                    pageNum++;
                }
            } finally {

                // 确保连接关闭
                if (connection != null && !connection.isClosed()) {
                    try {
                        connection.close();
                    } catch (SQLException ignored) {}
                }
            }
            
            logger.info("导出完成，总共导出{}条数据", totalExported);
            
        } catch (Exception e) {
            logger.error("导出试题数据时发生异常", e);
            throw e;
        } finally {
            // 确保ExcelWriter正确关闭，这会自动刷新和关闭输出流
            if (excelWriter != null) {
                try {
                    logger.info("开始关闭ExcelWriter");
                    excelWriter.finish();
                    logger.info("ExcelWriter关闭完成");
                } catch (Exception e) {
                    logger.error("关闭ExcelWriter时发生异常", e);
                    // 检查异常是否与流已关闭有关
                    if (e.getMessage() != null && e.getMessage().contains("Stream closed")) {
                        logger.warn("检测到流已关闭，可能是客户端已断开连接，这是正常现象");
                    } else {
                        // 对于其他异常，记录但不抛出，避免影响主流程
                        logger.warn("ExcelWriter关闭时发生非流关闭异常，但不会影响导出文件的完整性");
                    }
                }
            }
            // 注意：不需要手动刷新或关闭outputStream，因为excelWriter.finish()已经处理了
            logger.info("导出操作完成");
        }
    }

    /**
     * 将YeeQuestion列表转换为QuestionExportVO列表
     * @param questions 试题列表
     * @return 导出VO列表
     */
    private List<QuestionExportVO> convertToExportVO(List<YeeQuestion> questions) {
        List<QuestionExportVO> exportList = new ArrayList<>();
        
        for (YeeQuestion question : questions) {
            QuestionExportVO vo = new QuestionExportVO();
            
            // 基础字段
            vo.setTitle(question.getTitle());
            vo.setTopic(removeHtmlTags(question.getTopic()));
            vo.setType(question.getType());
            vo.setLevel(question.getLevel());
            vo.setScore(question.getScore());
            vo.setAnalysis(removeHtmlTags(question.getAnalysis()));
            vo.setScoreMode(question.getScoreMode());
            vo.setAddTime(question.getAddTime());
            
            // 类型名称转换
            vo.setTypeName(getTypeName(question.getType()));
            
            // 难度等级转换
            vo.setLevelName(getLevelName(question.getLevel()));
            
            // 处理选项和得分比
            processOptionsAndScores(vo, question);
            
            // 处理漏选分值（仅对多选题）
            if (question.getType() != null && question.getType() == 2 && question.getScoreMode() == 2) {
                processMissScores(vo, question);
            }

            
            // 设置计分模式名称（对多选题）
            if (question.getType() != null && (question.getType() == 2 )) {
                vo.setScoreModeName(getScoreModeName(question.getScoreMode()));
            }

            
            exportList.add(vo);
        }
        
        return exportList;
    }

    /**
     * 处理选项和得分比
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processOptionsAndScores(QuestionExportVO vo, YeeQuestion question) {
        // 特殊处理填空题
        if (question.getType() != null && question.getType() == 5) {
            processFillBlankOptions(vo, question);
            return;
        }
        
        if (question.getOption() == null || question.getOption().isEmpty()) {
            return;
        }

        List<Map<String, Object>> options = question.getOption();
        for (int i = 0; i < options.size() && i < 6; i++) { // 最多处理6个选项
            Map<String, Object> option = options.get(i);
//            String answer = removeHtmlTags((String) option.getOrDefault("answer", ""));
            String answer = (String) option.getOrDefault("answer", "");

            // 设置选项内容
            switch (i) {
                case 0: vo.setOptionA(answer); break;
                case 1: vo.setOptionB(answer); break;
                case 2: vo.setOptionC(answer); break;
                case 3: vo.setOptionD(answer); break;
                case 4: vo.setOptionE(answer); break;
                case 5: vo.setOptionF(answer); break;
            }
            
            // 设置得分比
            Object scaleObj = option.get("scale");
            String scale = scaleObj != null ? scaleObj.toString() : "0";
            
            switch (i) {
                case 0: vo.setScoreRatioA(Double.valueOf(scale)); break;
                case 1: vo.setScoreRatioB(Double.valueOf(scale)); break;
                case 2: vo.setScoreRatioC(Double.valueOf(scale)); break;
                case 3: vo.setScoreRatioD(Double.valueOf(scale)); break;
                case 4: vo.setScoreRatioE(Double.valueOf(scale)); break;
                case 5: vo.setScoreRatioF(Double.valueOf(scale)); break;
            }
        }
    }

    /**
     * 处理填空题选项
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processFillBlankOptions(QuestionExportVO vo, YeeQuestion question) {
        if (question.getOption() == null || question.getOption().isEmpty()) {
            return;
        }

        List<Map<String, Object>> options = question.getOption();
        // 按照idx排序填空题答案
        options.sort((o1, o2) -> {
            Object idx1 = o1.get("idx");
            Object idx2 = o2.get("idx");
            if (idx1 instanceof Integer && idx2 instanceof Integer) {
                return ((Integer) idx1).compareTo((Integer) idx2);
            } else if (idx1 instanceof String && idx2 instanceof String) {
                return ((String) idx1).compareTo((String) idx2);
            }
            return 0;
        });

        // 填充答案到选项字段
        for (int i = 0; i < options.size() && i < 6; i++) {
            Map<String, Object> option = options.get(i);
//            String answer = removeHtmlTags((String) option.getOrDefault("answer", ""));
            String answer = (String) option.getOrDefault("answer", "");

            switch (i) {
                case 0: vo.setOptionA(answer); break;
                case 1: vo.setOptionB(answer); break;
                case 2: vo.setOptionC(answer); break;
                case 3: vo.setOptionD(answer); break;
                case 4: vo.setOptionE(answer); break;
                case 5: vo.setOptionF(answer); break;
            }

            // 设置得分比
            Object scaleObj = option.get("scale");
            String scale = scaleObj != null ? scaleObj.toString() : "0";

            switch (i) {
                case 0: vo.setScoreRatioA(Double.valueOf(scale)); break;
                case 1: vo.setScoreRatioB(Double.valueOf(scale)); break;
                case 2: vo.setScoreRatioC(Double.valueOf(scale)); break;
                case 3: vo.setScoreRatioD(Double.valueOf(scale)); break;
                case 4: vo.setScoreRatioE(Double.valueOf(scale)); break;
                case 5: vo.setScoreRatioF(Double.valueOf(scale)); break;
            }
        }

    }

    /**
     * 处理漏选分值
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processMissScores(QuestionExportVO vo, YeeQuestion question) {
        if (question.getMissScore() == null || question.getMissScore().isEmpty()) {
            return;
        }
        List<Integer> missScores = question.getMissScore();
        for (int i = 0; i < missScores.size() && i < 5; i++)
            switch (i) {
                case 0: vo.setMissScore1(missScores.get(i).toString()); break;
                case 1: vo.setMissScore2(missScores.get(i).toString()); break;
                case 2: vo.setMissScore3(missScores.get(i).toString()); break;
                case 3: vo.setMissScore4(missScores.get(i).toString()); break;
                case 4: vo.setMissScore5(missScores.get(i).toString()); break;
            }

        
        // 设置计分模式名称
        if (question.getScoreMode() != null) {
            vo.setScoreModeName(getScoreModeName(question.getScoreMode()));
        }
    }

    /**
     * 获取题型名称
     * @param type 题型代码
     * @return 题型名称
     */
    private String getTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "单选";
            case 2: return "多选";
            case 3: return "判断";
            case 4: return "简答";
            case 5: return "填空";
            default: return "";
        }
    }

    /**
     * 获取难度等级名称
     * @param level 难度等级代码
     * @return 难度等级名称
     */
    private String getLevelName(Integer level) {
        if (level == null) return "";
        switch (level) {
            case 1: return "易";
            case 2: return "中";
            case 3: return "难";
            default: return "";
        }
    }

    /**
     * 获取计分模式名称
     * @param scoreMode 计分模式代码
     * @return 计分模式名称
     */
    private String getScoreModeName(Integer scoreMode) {
        if (scoreMode == null) return "";
        switch (scoreMode) {
            case 1: return "否";
            case 2: return "是";
            default: return "";
        }
    }

    /**
     * 移除HTML标签并保留空格和换行
     * @param html HTML内容
     * @return 纯文本内容
     */
    private String removeHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // 保留换行符和空格，移除其他HTML标签
        return html.replaceAll("<[^>]+>", "")
                  .replaceAll("&nbsp;", " ")
                  .replaceAll("&deg;", "°")
                  .replaceAll("&ldquo;", "\"")
                  .replaceAll("&rdquo;", "\"")
                  .replaceAll("&lt;", "<")
                  .replaceAll("&gt;", ">")
                  .replaceAll("&amp;", "&")
                  .replaceAll("\\s+", " ") // 合并多个空白字符为一个空格
                  .trim();
    }
}