package cn.xfywz.guozespring.service.student.serviceImpl;


import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.*;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.YeeStudentCourseExamService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.util.ScoreCalculator;
import cn.xfywz.guozespring.config.RabbitMQConfig;
import cn.xfywz.guozespring.entity.dto.ExamScoreMessage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.springframework.jdbc.support.JdbcUtils.closeStatement;

@Slf4j
@Service
public class YeeStudentCourseExamServiceImpl implements YeeStudentCourseExamService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 学生课程考试列表
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param nodeId
     * @return Result
     * @throws Exception
     */
    @Transactional
    @Override
    public Result selectStudentExamList(int schoolId, Integer courseId, Integer studentId, Integer nodeId) throws Exception {
        // 获取作业信息
        List<Map<String, Object>> examInfo = getExamInfoByCourseAndStudent(schoolId, courseId, studentId, nodeId);

        // 循环获取作业信息 答题次数、进入权限、状态提示
        for (Map<String, Object> work : examInfo) {
            Integer examId = (Integer) work.get("examId");
            // 1. 获取答题次数
            Integer frequency = getExamFrequencyByUserAndWork(schoolId, studentId, examId);
            work.put("frequency", frequency);

            // 2. 查询该学生本场考试记录
            List<YeeExamRecord> recordList = getExistingExamRecords(schoolId, examId, studentId);
            boolean canEnter;
            String enterTip;
            Integer examStatus; // 0无记录/1进行中/2已交卷

            if (recordList == null || recordList.isEmpty()) {
                // 无记录：初次可进入
                canEnter = true;
                enterTip = "可初次进入考试";
                examStatus = 0;
            } else {
                YeeExamRecord record = recordList.get(0);
                if (Objects.equals(record.getState(), 1)) {
                    // 存在未交卷记录，可再次进入
                    canEnter = true;
                    enterTip = "可继续上次答题";
                    examStatus = 1;
                } else {
                    // 已交卷，禁止进入
                    canEnter = false;
                    enterTip = "试卷已提交，不可重复进入";
                    examStatus = 2;
                }
            }
            // 塞入前端列表展示字段
            work.put("canEnter", canEnter);
            work.put("enterTip", enterTip);
            work.put("examStatus", examStatus);
        }

        Map resultMap = new HashMap<>();
        resultMap.put("examInfo", examInfo);

        return Result.success(resultMap);
    }

    /**
     * 学生课程考试详情
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param workId
     * @return Result
     * @throws Exception
     */
    @Transactional
    @Override
    public Result selectStudentExamDetail(int schoolId, Integer courseId, Integer studentId, Integer workId, String title) throws Exception {
        // 第一部分 信息
        List<Map<String, Object>> workDetail = getStudentExamInfoByCourseAndStudent(schoolId, courseId, studentId);

        // 第二部分 信息过滤 只保留 title对应的 信息 stream流
        workDetail = workDetail.stream()
                .filter(work -> work.get("title").equals(title))
                .collect(Collectors.toList());

        Map resultMap = new HashMap<>();
        resultMap.put("workDetail", workDetail);

        return Result.success(resultMap);
    }


    /**
     * 考试开始答题
     * @param schoolId
     * @param courseId
     * @param userId
     * @param workId
     * @return
     */
//    @Transactional
//    @Override
//    public Result startExam(int schoolId, Integer courseId, Integer userId, Integer workId,
//                            Integer createUserId, String platform, Integer classId, Integer paperId,
//                            Integer random, String stringRandData, Integer randNumber) throws Exception {
//
//        List<YeeExamRecord> existingRecords = getExistingExamRecords(schoolId, workId, userId);
//        if (existingRecords != null && !existingRecords.isEmpty()) {
//            YeeExamRecord activeRecord = existingRecords.get(0);
//            Integer currRecordId = activeRecord.getId();
//            // 已提交禁止重进
//            if (!Objects.equals(activeRecord.getState(), 1)) {
//                return Result.error("试卷已提交，无法再次进入答题");
//            }
//            long nowSec = System.currentTimeMillis() / 1000;
//            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//            updateRecordLastActiveJdbc(slSchool, currRecordId, (int) nowSec);
//
//            // 1、优先从作答表查询题目ID（有作答时保留原始答题顺序+已填答案）
//            List<Integer> topicIdList = queryTopicIdByRecordIdJdbc(slSchool, currRecordId);
//
//            // 兜底：作答表无数据读取主表存储的原始抽题ID
//            if (topicIdList.isEmpty()) {
//                String topicIdJson = activeRecord.getSelectTopicIds();
//                if (topicIdJson != null && !topicIdJson.isBlank()) {
//                    try {
//                        topicIdList = JSON.parseArray(topicIdJson, Integer.class);
//                    } catch (Exception e) {
//                        topicIdList = new ArrayList<>();
//                    }
//                }
//            }
//
//            if (topicIdList.isEmpty()) {
//                return Result.error("本场考试题目数据异常，请退出后重新创建考试记录");
//            }
//            // 2、查询原始完整题目信息
//            List<Map<String, Object>> rawTopicList = queryTopicByIdListJdbc(slSchool, topicIdList);
//            // 还原原始抽题顺序
//            rawTopicList = sortTopicByTopicIdList(topicIdList, rawTopicList);
//            // 校验题目是否缺失
//            if (rawTopicList.size() != topicIdList.size()) {
//                return Result.error("本场考试题目数据缺失，请退出后重新创建考试记录");
//            }
//            // 3、查询本场所有历史作答
//            List<Map<String, Object>> answerList = queryAllAnswerByRecordIdJdbc(slSchool, currRecordId);
//            Map<Integer, Map<String, Object>> answerMap = new HashMap<>();
//            for (Map<String, Object> ans : answerList) {
//                Integer tid = (Integer) ans.get("topicId");
//                answerMap.put(tid, ans);
//            }
//
//            List<Map<String, Object>> formatTopicList = new ArrayList<>();
//            int numberSeq = 1;
//            for (Map<String, Object> rawTopic : rawTopicList) {
//                Map<String, Object> item = new HashMap<>();
//                Integer tid = (Integer) rawTopic.get("id");
//
//                item.put("recordId", currRecordId);
//                item.put("number", numberSeq++);
//                item.put("score", rawTopic.get("score"));
//                item.put("examId", rawTopic.get("examId"));
//                item.put("topic", rawTopic.get("topic"));
//                item.put("id", tid);
//                item.put("type", rawTopic.get("type"));
//
//                // 题目选项JSON转数组
//                String optionJson = (String) rawTopic.get("option");
//                List<Map<String, Object>> optionArr = new ArrayList<>();
//                if (optionJson != null && !optionJson.isBlank()) {
//                    try {
//                        List<?> tempOpt = JSON.parseArray(optionJson, Map.class);
//                        @SuppressWarnings("unchecked")
//                        List<Map<String, Object>> castOpt = (List<Map<String, Object>>) tempOpt;
//                        optionArr = castOpt;
//                    } catch (Exception e) {
//                        optionArr = new ArrayList<>();
//                    }
//                }
//                item.put("option", optionArr);
//
//                // 匹配作答记录
//                Map<String, Object> ansRow = answerMap.get(tid);
//                if (ansRow != null) {
//                    item.put("answer", ansRow.get("answer"));
//                    item.put("images", ansRow.get("images"));
//                    item.put("files", ansRow.get("files"));
//                    item.put("hit", ansRow.get("hit"));
//                    item.put("marked", ansRow.get("marked"));
//                } else {
//                    item.put("answer", null);
//                    item.put("images", null);
//                    item.put("files", null);
//                    item.put("hit", 0);
//                    item.put("marked", "0");
//                }
//                formatTopicList.add(item);
//            }
//
//            Map resultMap = new HashMap<>();
//            resultMap.put("examTopics", formatTopicList);
//            return Result.success(resultMap);
//        }
//        // ======================================================================================
//
//        // 查询考试和课程配置，校验时间窗口
//        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            return Result.error("学校不存在或未审核");
//        }
//        YeeExam exam;
//        YeeCourse course;
//        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
//            exam = queryExamById(conn, workId);
//            if (exam == null) {
//                return Result.error("考试不存在");
//            }
//            course = queryCourseById(conn, courseId);
//        }
//        long nowSec = System.currentTimeMillis() / 1000;
//        if (exam.getStartTime() != null && exam.getStartTime() > 0 && nowSec < exam.getStartTime()) {
//            return Result.error("考试尚未开始");
//        }
//        if (exam.getEndTime() != null && exam.getEndTime() > 0 && nowSec > exam.getEndTime()) {
//            return Result.error("考试已结束");
//        }
//        if (course != null && course.getStartDate() != null && course.getStartDate().after(new java.util.Date())) {
//            return Result.error("课程尚未开课");
//        }
//
//        // 随机抽题参数 fallback
//        Integer effectiveRandom = (random != null) ? random : exam.getRandom();
//        String effectiveRandData = stringRandData;
//        if (effectiveRandData == null && exam.getRandData() != null) {
//            effectiveRandData = JSON.toJSONString(exam.getRandData());
//        }
//        Integer effectiveRandNumber = (randNumber != null) ? randNumber : exam.getRandNumber();
//
//        Map resultMap = new HashMap<>();
//
//        // 1. 构建考试记录实体
//        YeeExamRecord yeeWorkRecord = new YeeExamRecord();
//        yeeWorkRecord.setSchoolId(schoolId);
//        yeeWorkRecord.setExamId(workId);
//        yeeWorkRecord.setUserid(userId);
//        yeeWorkRecord.setStartTime((int) nowSec);
//        yeeWorkRecord.setState(1); // 1=未提交
//        yeeWorkRecord.setFinishTime(0);
//        yeeWorkRecord.setScore(new BigDecimal(0));
//        yeeWorkRecord.setIsCancel(0);
//        yeeWorkRecord.setFrequency(1);
//        yeeWorkRecord.setTeacherId(createUserId);
//        yeeWorkRecord.setMarkTime(0);
//        yeeWorkRecord.setObScore(new BigDecimal(0));
//        yeeWorkRecord.setSubScore(new BigDecimal(0));
//        yeeWorkRecord.setMarkOrder(0);
//        yeeWorkRecord.setPlatform(platform);
//        yeeWorkRecord.setCourseId(courseId);
//        yeeWorkRecord.setClassId(classId);
//        yeeWorkRecord.setSubmitType(0);
//        yeeWorkRecord.setSubmitTime(0);
//        yeeWorkRecord.setLastActiveTime((int) nowSec);
//
//        // 查询试卷原始题目
//        List<Map<String, Object>> maps = queryPaperTopicsAsMap(schoolId, workId, userId, 0);
//        if (maps.isEmpty()) {
//            return Result.error("没有此试卷");
//        }
//        for (Map<String, Object> map : maps) {
//            map.put("recordId", 0);
//        }
//
//        // 随机抽题逻辑
//        if (effectiveRandom != null && effectiveRandom == 1) {
//            if (effectiveRandData == null || effectiveRandData.trim().isEmpty()) {
//                return Result.error("请重新设置抽题规则");
//            }
//            Map<String, Integer> randDataMap;
//            try {
//                randDataMap = JSON.parseObject(effectiveRandData, new TypeReference<Map<String, Integer>>() {});
//            } catch (Exception e) {
//                return Result.error("请重新设置抽题规则");
//            }
//            if (randDataMap == null) {
//                return Result.error("请重新设置抽题规则");
//            }
//            Map<Integer, Integer> typeToCountMap = new HashMap<>();
//            typeToCountMap.put(1, randDataMap.getOrDefault("t1", 0));
//            typeToCountMap.put(2, randDataMap.getOrDefault("t2", 0));
//            typeToCountMap.put(3, randDataMap.getOrDefault("t3", 0));
//            typeToCountMap.put(5, randDataMap.getOrDefault("t4", 0));
//            typeToCountMap.put(4, randDataMap.getOrDefault("t5", 0));
//
//            Map<Integer, List<Map<String, Object>>> questionsByType = maps.stream()
//                    .collect(Collectors.groupingBy(question -> (Integer) question.get("type")));
//            List<Map<String, Object>> filteredQuestions = new ArrayList<>();
//            Random randomGenerator = ThreadLocalRandom.current();
//            for (Map.Entry<Integer, Integer> entry : typeToCountMap.entrySet()) {
//                Integer type = entry.getKey();
//                Integer needCount = entry.getValue();
//                if (needCount <= 0) continue;
//                List<Map<String, Object>> typeQuestions = questionsByType.get(type);
//                if (typeQuestions == null || typeQuestions.isEmpty()) continue;
//                Collections.shuffle(typeQuestions, randomGenerator);
//                int realCount = Math.min(needCount, typeQuestions.size());
//                filteredQuestions.addAll(typeQuestions.subList(0, realCount));
//            }
//            if (filteredQuestions.isEmpty()) {
//                return Result.error("抽题规则配置无效，请重新设置抽题规则");
//            }
//            maps = filteredQuestions;
//        }
//
//        // 【核心修复1：先给实体赋值抽题ID，再执行插入】
//        List<Integer> originTopicIds = maps.stream()
//                .map(m -> (Integer) m.get("id"))
//                .collect(Collectors.toList());
//        yeeWorkRecord.setSelectTopicIds(JSON.toJSONString(originTopicIds));
//
//        // 【核心修复2：赋值完成后再插入数据库】
//        int recordId = insertYeeExamRecord(yeeWorkRecord);
//        if (recordId == -1) {
//            return Result.error("开始考试失败: yee_exam_record 插入失败");
//        }
//
//        // 回填recordId到题目
//        for (Map<String, Object> map : maps) {
//            map.put("recordId", recordId);
//        }
//        resultMap.put("examTopics", maps);
//
//        // 初始化 yee_exam_answer
//        List<YeeExamAnswer> yeeWorkAnswers = maps.stream()
//                .map(map -> {
//                    YeeExamAnswer yeeWorkAnswer = new YeeExamAnswer();
//                    yeeWorkAnswer.setRecordId(recordId);
//                    yeeWorkAnswer.setExamId(workId);
//                    yeeWorkAnswer.setTopicId((Integer) map.get("id"));
//                    yeeWorkAnswer.setAnswered(0);
//                    yeeWorkAnswer.setScore(new BigDecimal(0));
//                    yeeWorkAnswer.setAnswer(null);
//                    yeeWorkAnswer.setImages(null);
//                    yeeWorkAnswer.setFiles(null);
//                    yeeWorkAnswer.setMarked("0");
//                    yeeWorkAnswer.setHit(0);
//                    yeeWorkAnswer.setUserId(userId);
//                    yeeWorkAnswer.setCourseId(courseId);
//                    yeeWorkAnswer.setSchoolId(schoolId);
//                    return yeeWorkAnswer;
//                }).collect(Collectors.toList());
//
//        boolean result = insertYeeWorkAnswers(yeeWorkAnswers);
//        if (!result) {
//            return Result.error("开始考试失败: yee_exam_answer 插入失败");
//        }
//
//        // 插入成绩记录
//        insertYeeWorkScore(workId, userId, courseId, schoolId, platform);
//
//        return Result.success(resultMap);
//    }
    /**
     * 考试开始答题【修复事务连接问题完整版】
     * @param schoolId
     * @param courseId
     * @param userId
     * @param workId
     * @return
     */
    @Transactional
    @Override
    public Result startExam(int schoolId, Integer courseId, Integer userId, Integer workId,
                            Integer createUserId, String platform, Integer classId, Integer paperId,
                            Integer random, String stringRandData, Integer randNumber) throws Exception {

        List<YeeExamRecord> existingRecords = getExistingExamRecords(schoolId, workId, userId);
        if (existingRecords != null && !existingRecords.isEmpty()) {
            YeeExamRecord activeRecord = existingRecords.get(0);
            Integer currRecordId = activeRecord.getId();
            // 已提交禁止重进
            if (!Objects.equals(activeRecord.getState(), 1)) {
                return Result.error("试卷已提交，无法再次进入答题");
            }
            long nowSec = System.currentTimeMillis() / 1000;
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            updateRecordLastActiveJdbc(slSchool, currRecordId, (int) nowSec);

            // 1、优先从作答表查询题目ID（有作答时保留原始答题顺序+已填答案）
            List<Integer> topicIdList = queryTopicIdByRecordIdJdbc(slSchool, currRecordId);

            // 兜底：作答表无数据读取主表存储的原始抽题ID
            if (topicIdList.isEmpty()) {
                String topicIdJson = activeRecord.getSelectTopicIds();
                if (topicIdJson != null && !topicIdJson.isBlank()) {
                    try {
                        topicIdList = JSON.parseArray(topicIdJson, Integer.class);
                    } catch (Exception e) {
                        log.error("解析考试记录selectTopicIds JSON失败 recordId={}, json={}", currRecordId, topicIdJson, e);
                        topicIdList = new ArrayList<>();
                    }
                }
            }

            // 兜底校验，无题目直接提示重建记录
            if (topicIdList.isEmpty()) {
                return Result.error("本场考试题目数据异常，请退出后重新创建考试记录");
            }
            // 2、查询原始完整题目信息
            List<Map<String, Object>> rawTopicList = queryTopicByIdListJdbc(slSchool, topicIdList);
            // 还原原始抽题顺序
            rawTopicList = sortTopicByTopicIdList(topicIdList, rawTopicList);
            // 校验题目是否缺失
            if (rawTopicList.size() != topicIdList.size()) {
                return Result.error("本场考试题目数据缺失，请退出后重新创建考试记录");
            }
            // 3、查询本场所有历史作答
            List<Map<String, Object>> answerList = queryAllAnswerByRecordIdJdbc(slSchool, currRecordId);
            Map<Integer, Map<String, Object>> answerMap = new HashMap<>();
            for (Map<String, Object> ans : answerList) {
                Integer tid = (Integer) ans.get("topicId");
                answerMap.put(tid, ans);
            }

            List<Map<String, Object>> formatTopicList = new ArrayList<>();
            int numberSeq = 1;
            for (Map<String, Object> rawTopic : rawTopicList) {
                Map<String, Object> item = new HashMap<>();
                Integer tid = (Integer) rawTopic.get("id");

                item.put("recordId", currRecordId);
                item.put("number", numberSeq++);
                item.put("score", rawTopic.get("score"));
                item.put("examId", rawTopic.get("examId"));
                item.put("topic", rawTopic.get("topic"));
                item.put("id", tid);
                item.put("type", rawTopic.get("type"));

                // 题目选项JSON转数组
                String optionJson = (String) rawTopic.get("option");
                List<Map<String, Object>> optionArr = new ArrayList<>();
                if (optionJson != null && !optionJson.isBlank()) {
                    try {
                        List<?> tempOpt = JSON.parseArray(optionJson, Map.class);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> castOpt = (List<Map<String, Object>>) tempOpt;
                        optionArr = castOpt;
                    } catch (Exception e) {
                        optionArr = new ArrayList<>();
                    }
                }
                item.put("option", optionArr);

                // 匹配作答记录
                Map<String, Object> ansRow = answerMap.get(tid);
                if (ansRow != null) {
                    item.put("answer", ansRow.get("answer"));
                    item.put("images", ansRow.get("images"));
                    item.put("files", ansRow.get("files"));
                    item.put("hit", ansRow.get("hit"));
                    item.put("marked", ansRow.get("marked"));
                } else {
                    item.put("answer", null);
                    item.put("images", null);
                    item.put("files", null);
                    item.put("hit", 0);
                    item.put("marked", "0");
                }
                formatTopicList.add(item);
            }

            Map resultMap = new HashMap<>();
            resultMap.put("examTopics", formatTopicList);
            return Result.success(resultMap);
        }
        // ======================================================================================

        // 查询考试和课程配置，校验时间窗口
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        YeeExam exam;
        YeeCourse course;
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            exam = queryExamById(conn, workId);
            if (exam == null) {
                return Result.error("考试不存在");
            }
            course = queryCourseById(conn, courseId);
        }
        long nowSec = System.currentTimeMillis() / 1000;
        if (exam.getStartTime() != null && exam.getStartTime() > 0 && nowSec < exam.getStartTime()) {
            return Result.error("考试尚未开始");
        }
        if (exam.getEndTime() != null && exam.getEndTime() > 0 && nowSec > exam.getEndTime()) {
            return Result.error("考试已结束");
        }
        if (course != null && course.getStartDate() != null && course.getStartDate().after(new java.util.Date())) {
            return Result.error("课程尚未开课");
        }

        // 随机抽题参数 fallback（从试卷YeeExam取，无任何实体报错）
        Integer effectiveRandom = (random != null) ? random : exam.getRandom();
        String effectiveRandData = stringRandData;
        if (effectiveRandData == null && exam.getRandData() != null) {
            effectiveRandData = JSON.toJSONString(exam.getRandData());
        }
        Integer effectiveRandNumber = (randNumber != null) ? randNumber : exam.getRandNumber();

        Map resultMap = new HashMap<>();

        // 1. 构建考试记录实体
        YeeExamRecord yeeWorkRecord = new YeeExamRecord();
        yeeWorkRecord.setSchoolId(schoolId);
        yeeWorkRecord.setExamId(workId);
        yeeWorkRecord.setUserid(userId);
        yeeWorkRecord.setStartTime((int) nowSec);
        yeeWorkRecord.setState(1);
        yeeWorkRecord.setFinishTime(0);
        yeeWorkRecord.setScore(new BigDecimal(0));
        yeeWorkRecord.setIsCancel(0);
        yeeWorkRecord.setFrequency(1);
        yeeWorkRecord.setTeacherId(createUserId);
        yeeWorkRecord.setMarkTime(0);
        yeeWorkRecord.setObScore(new BigDecimal(0));
        yeeWorkRecord.setSubScore(new BigDecimal(0));
        yeeWorkRecord.setMarkOrder(0);
        yeeWorkRecord.setPlatform(platform);
        yeeWorkRecord.setCourseId(courseId);
        yeeWorkRecord.setClassId(classId);
        yeeWorkRecord.setSubmitType(0);
        yeeWorkRecord.setSubmitTime(0);
        yeeWorkRecord.setLastActiveTime((int) nowSec);

        // 查询试卷原始题目
        List<Map<String, Object>> maps = queryPaperTopicsAsMap(schoolId, workId, userId, 0);
        // 固定试卷空题目拦截，根源防止脏记录入库
        if (maps.isEmpty()) {
            return Result.error("没有此试卷");
        }
        for (Map<String, Object> map : maps) {
            map.put("recordId", 0);
        }

        // 随机抽题逻辑
        if (effectiveRandom != null && effectiveRandom == 1) {
            if (effectiveRandData == null || effectiveRandData.trim().isEmpty()) {
                return Result.error("请重新设置抽题规则");
            }
            Map<String, Integer> randDataMap;
            try {
                randDataMap = JSON.parseObject(effectiveRandData, new TypeReference<Map<String, Integer>>() {});
            } catch (Exception e) {
                return Result.error("请重新设置抽题规则");
            }
            if (randDataMap == null) {
                return Result.error("请重新设置抽题规则");
            }
            Map<Integer, Integer> typeToCountMap = new HashMap<>();
            typeToCountMap.put(1, randDataMap.getOrDefault("t1", 0));
            typeToCountMap.put(2, randDataMap.getOrDefault("t2", 0));
            typeToCountMap.put(3, randDataMap.getOrDefault("t3", 0));
            typeToCountMap.put(5, randDataMap.getOrDefault("t4", 0));
            typeToCountMap.put(4, randDataMap.getOrDefault("t5", 0));

            Map<Integer, List<Map<String, Object>>> questionsByType = maps.stream()
                    .collect(Collectors.groupingBy(question -> (Integer) question.get("type")));
            List<Map<String, Object>> filteredQuestions = new ArrayList<>();
            Random randomGenerator = ThreadLocalRandom.current();
            for (Map.Entry<Integer, Integer> entry : typeToCountMap.entrySet()) {
                Integer type = entry.getKey();
                Integer needCount = entry.getValue();
                if (needCount <= 0) continue;
                List<Map<String, Object>> typeQuestions = questionsByType.get(type);
                if (typeQuestions == null || typeQuestions.isEmpty()) continue;
                Collections.shuffle(typeQuestions, randomGenerator);
                int realCount = Math.min(needCount, typeQuestions.size());
                filteredQuestions.addAll(typeQuestions.subList(0, realCount));
            }
            if (filteredQuestions.isEmpty()) {
                return Result.error("抽题规则配置无效，请重新设置抽题规则");
            }
            maps = filteredQuestions;
        }

        // 【核心修复：双重校验originTopicIds非空，杜绝空数组存入数据库脏数据】
        List<Integer> originTopicIds = maps.stream()
                .map(m -> (Integer) m.get("id"))
                .collect(Collectors.toList());
        // 新增空值拦截
        if (originTopicIds.isEmpty()) {
            return Result.error("试卷题目加载失败，无有效题目");
        }
        yeeWorkRecord.setSelectTopicIds(JSON.toJSONString(originTopicIds));

        // ====================== 事务连接统一复用核心修改 ======================
        Connection transConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        transConn.setAutoCommit(false);
        Integer recordId = null;
        try {
            // 1. 插入record，传入事务连接
            recordId = insertYeeExamRecord(yeeWorkRecord, transConn);
            if (recordId == -1) {
                throw new Exception("yee_exam_record 插入失败");
            }
            final Integer finalRecordId = recordId;

            for (Map<String, Object> map : maps) {
                map.put("recordId", recordId);
            }
            resultMap.put("examTopics", maps);

            List<YeeExamAnswer> yeeWorkAnswers = maps.stream()
                    .map(map -> {
                        YeeExamAnswer yeeWorkAnswer = new YeeExamAnswer();
                        yeeWorkAnswer.setRecordId(finalRecordId);
                        yeeWorkAnswer.setExamId(workId);
                        yeeWorkAnswer.setTopicId((Integer) map.get("id"));
                        yeeWorkAnswer.setAnswered(0);
                        yeeWorkAnswer.setScore(new BigDecimal(0));
                        yeeWorkAnswer.setAnswer(null);
                        yeeWorkAnswer.setImages(null);
                        yeeWorkAnswer.setFiles(null);
                        yeeWorkAnswer.setMarked("0");
                        yeeWorkAnswer.setHit(0);
                        yeeWorkAnswer.setUserId(userId);
                        yeeWorkAnswer.setCourseId(courseId);
                        yeeWorkAnswer.setSchoolId(schoolId);
                        return yeeWorkAnswer;
                    }).collect(Collectors.toList());

            // 2. 批量插入answer，共用事务连接
            boolean result = insertYeeWorkAnswers(yeeWorkAnswers, transConn);
            if (!result) {
                throw new Exception("yee_exam_answer 插入失败");
            }

            // 3. 插入成绩记录，共用事务连接
            insertYeeWorkScore(workId, userId, courseId, schoolId, platform, transConn);

            // 全部执行成功提交
            transConn.commit();
        } catch (Exception e) {
            // 任意步骤异常整体回滚，彻底杜绝半截入库
            transConn.rollback();
            log.error("创建考试记录事务回滚 schoolId={}, examId={}, userId={}", schoolId, workId, userId, e);
            if (recordId != null) {
                return Result.error("开始考试失败: yee_exam_answer 插入失败");
            } else {
                return Result.error("开始考试失败: yee_exam_record 插入失败");
            }
        } finally {
            transConn.setAutoCommit(true);
            closeConnection(transConn);
        }

        return Result.success(resultMap);
    }
    /**
     * 将IN查询返回的无序题目列表，按answer表原始topicId顺序重排
     * @param sourceTopicIdList 从yee_*_answer取出的有序题目ID（原始抽题顺序）
     * @param unSortTopicList IN查询出来的乱序题目集合
     * @return 和ID顺序一一对应的有序题目列表
     */
    private List<Map<String, Object>> sortTopicByTopicIdList(List<Integer> sourceTopicIdList, List<Map<String, Object>> unSortTopicList) {
        Map<Integer, Map<String, Object>> topicIdMap = new HashMap<>(unSortTopicList.size());
        for (Map<String, Object> topicRow : unSortTopicList) {
            Integer tid = (Integer) topicRow.get("id");
            topicIdMap.put(tid, topicRow);
        }
        List<Map<String, Object>> sortedTopicList = new ArrayList<>(sourceTopicIdList.size());
        for (Integer tid : sourceTopicIdList) {
            Map<String, Object> targetTopic = topicIdMap.get(tid);
            if (targetTopic != null) {
                sortedTopicList.add(targetTopic);
            }
        }
        return sortedTopicList;
    }    /**
     * 根据recordId查询本场所有题目的作答记录
     */
    private List<Map<String, Object>> queryAllAnswerByRecordIdJdbc(SlSchool slSchool, Integer recordId) throws Exception {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT topicId, answer, images, files, hit, marked FROM yee_exam_answer WHERE recordId = ?";
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, recordId);
            rs = pstmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
            }
        } finally {
            closeResultSetAndStatement(rs, pstmt);
            closeConnection(conn);
        }
        return result;
    }    /**
     * 更新记录最后活跃时间
     */
    private void updateRecordLastActiveJdbc(SlSchool slSchool, Integer recordId, int nowSec) throws Exception {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "UPDATE yee_exam_record SET lastActiveTime = ? WHERE id = ?";
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, nowSec);
            pstmt.setInt(2, recordId);
            pstmt.executeUpdate();
        } finally {
            closeStatement(pstmt);
            closeConnection(conn);
        }
    }
// ===================== 新增2个安全JDBC工具方法（替代旧的字符串拼接查询） =====================
    /**
     * 根据recordId从yee_exam_answer查询本场所有题目ID，按抽题原始顺序返回
     */
    private List<Integer> queryTopicIdByRecordIdJdbc(SlSchool slSchool, Integer recordId) throws Exception {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Integer> idList = new ArrayList<>();
        String sql = "SELECT id AS answerRowId,topicId FROM yee_exam_answer WHERE recordId = ? ORDER BY id ASC";
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, recordId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                idList.add(rs.getInt("topicId"));
            }
        } finally {
            closeResultSetAndStatement(rs, pstmt);
            closeConnection(conn);
        }
        return idList;
    }

    /**
     * 根据题目ID列表批量查询题目详情，使用占位符杜绝SQL注入
     */
    private List<Map<String, Object>> queryTopicByIdListJdbc(SlSchool slSchool, List<Integer> idList) throws Exception {
        if (idList.isEmpty()) return new ArrayList<>();
        String[] placeholderArr = new String[idList.size()];
        Arrays.fill(placeholderArr, "?");
        String placeholders = String.join(",", placeholderArr);
        String sql = "SELECT id, topic, type, score, `option`, examId FROM yee_exam_topic WHERE id IN (" + placeholders + ")";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < idList.size(); i++) {
                pstmt.setInt(i + 1, idList.get(i));
            }
            rs = pstmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
            }
        } finally {
            closeResultSetAndStatement(rs, pstmt);
            closeConnection(conn);
        }
        return result;
    }

    /**
     * 查询已有考试记录
     * @param schoolId 学校ID
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 已有考试记录列表
     * @throws Exception 查询失败
     */
    private List<YeeExamRecord> getExistingExamRecords(Integer schoolId, Integer examId, Integer userId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<YeeExamRecord> resultList = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 2. 获取从库连接（读操作）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. SQL 查询语句
            String sql = "SELECT * FROM yee_exam_record WHERE schoolId = ? AND examId = ? AND userId = ?";

            st = conn.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, examId);
            st.setInt(3, userId);

            // 4. 执行查询
            rs = st.executeQuery();

            // 5. 封装结果
            while (rs.next()) {
                YeeExamRecord record = new YeeExamRecord();
                record.setId(rs.getInt("id"));
                record.setExamId(rs.getInt("examId"));
                record.setUserid(rs.getInt("userId"));
                record.setStartTime(rs.getInt("startTime"));
                record.setState(rs.getInt("state"));
                record.setFinishTime(rs.getInt("finishTime"));
                record.setScore(rs.getBigDecimal("score"));
                record.setIsCancel(rs.getInt("isCancel"));
                record.setFrequency(rs.getInt("frequency"));
                record.setTeacherId(rs.getInt("teacherId"));
                record.setMarkTime(rs.getInt("markTime"));
                record.setObScore(rs.getBigDecimal("obScore"));
                record.setSubScore(rs.getBigDecimal("subScore"));
                record.setMarkOrder(rs.getInt("markOrder"));
                record.setPlatform(rs.getString("platform"));
                record.setCourseId(rs.getInt("courseId"));
                record.setClassId(rs.getInt("classId"));
                record.setSchoolId(rs.getInt("schoolId"));
                record.setAddDate(rs.getDate("addDate"));

                resultList.add(record);
            }

            return resultList;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询 yee_exam_record 失败，条件: schoolId=" + schoolId +
                    ", examId=" + examId +
                    ", userId=" + userId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 添加作业答案
     * @param schoolId
     * @param courseId
     * @param userId
     * @param answer
     * @param topicId
     * @param examId
     * @param recordId
     * @return
     */
    @Override
    public Result addExamAnswer(int schoolId, Integer courseId, Integer userId, List<String> answer, Integer topicId, Integer examId, Integer recordId, Integer type) throws Exception {
        log.info("保存考试答案(客观题): schoolId={}, courseId={}, userId={}, examId={}, recordId={}, topicId={}, type={}",
                schoolId, courseId, userId, examId, recordId, topicId, type);
        try {
            boolean result = updateYeeWorkAnswer(schoolId, courseId, userId, answer, topicId, examId, recordId, type);
            if (result) {
                if (schoolId > 0 && courseId != null && userId != null && examId != null && recordId != null && topicId != null) {
                    calculateSingleTopicScore(schoolId, courseId, userId, examId, recordId, topicId);
                }
            }
            return result ? Result.success("answer添加成功") : Result.error("answer添加失败");
        } catch (Exception e) {
            log.error("保存考试答案(客观题)失败: schoolId={}, examId={}, recordId={}, topicId={}",
                    schoolId, examId, recordId, topicId, e);
            throw e;
        }
    }

    /**
     * 添加作业答案
     * @param schoolId
     * @param courseId
     * @param userId
     * @param answer
     * @param topicId
     * @param workId
     * @param recordId
     * @return
     */
    @Override
    public Result addExamAnswerText(int schoolId, Integer courseId, Integer userId, String answer, Integer topicId, Integer workId, Integer recordId, Integer type, List<FileInfo> images, List<FileInfo> files) throws Exception  {
        log.info("保存考试答案(主观题): schoolId={}, courseId={}, userId={}, workId={}, recordId={}, topicId={}, type={}",
                schoolId, courseId, userId, workId, recordId, topicId, type);
        try {
            boolean result = updateYeeWorkAnswerText(schoolId, courseId, userId, answer, topicId, workId, recordId, type, images, files);
            if (result) {
                if (schoolId > 0 && courseId != null && userId != null && workId != null && recordId != null && topicId != null) {
                    calculateSingleTopicScore(schoolId, courseId, userId, workId, recordId, topicId);
                }
            }
            return result ? Result.success("answer添加成功") : Result.error("answer添加失败");
        } catch (Exception e) {
            log.error("保存考试答案(主观题)失败: schoolId={}, workId={}, recordId={}, topicId={}",
                    schoolId, workId, recordId, topicId, e);
            throw e;
        }
    }

    /**
     * 添加作业答案
     * @param schoolId
     * @param courseId
     * @param userId
     * @param answer
     * @param topicId
     * @param workId
     * @param recordId
     * @return
     */
    @Override
    public Result addExamAnswerBlank(int schoolId, Integer courseId, Integer userId, Map<String, String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception {
        log.info("保存考试答案(填空题): schoolId={}, courseId={}, userId={}, workId={}, recordId={}, topicId={}, type={}",
                schoolId, courseId, userId, workId, recordId, topicId, type);
        try {
            boolean result = updateYeeWorkAnswerBlank(schoolId, courseId, userId, answer, topicId, workId, recordId, type);
            if (result) {
                if (schoolId > 0 && courseId != null && userId != null && workId != null && recordId != null && topicId != null) {
                    calculateSingleTopicScore(schoolId, courseId, userId, workId, recordId, topicId);
                }
            }
            return result ? Result.success("answer添加成功") : Result.error("answer添加失败");
        } catch (Exception e) {
            log.error("保存考试答案(填空题)失败: schoolId={}, workId={}, recordId={}, topicId={}",
                    schoolId, workId, recordId, topicId, e);
            throw e;
        }
    }

    /**
     * 完成答题
     * @param schoolId
     * @param courseId
     * @param userId
     * @param workId
     * @param recordId
     * @return
     * @throws Exception
     */
    @Override
    public Result finishExamAnswer(int schoolId, Integer courseId, Integer userId, Integer workId, Integer recordId) throws Exception {
        log.info("交卷开始: schoolId={}, courseId={}, userId={}, examId={}, recordId={}",
                schoolId, courseId, userId, workId, recordId);
        try {
            // 1. 获取题目
            List<Map<String, Object>> maps = queryStudentExamTopicsByRecordId(schoolId, recordId);
            log.info("交卷-题目数量: {}", maps.size());

            // 2. 获取答案
            List<YeeExamAnswer> yeeWorkAnswers = queryYeeWorkAnswers(schoolId, recordId, workId, userId, courseId);
            log.info("交卷-答案数量: {}", yeeWorkAnswers.size());

            // 3. 从yeeWorkAnswers中提取 topic和answer字段
            List<Map<String, Object>> yeeWorkAnswersMap = yeeWorkAnswers.stream()
                    .map(yeeWorkAnswer -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", String.valueOf(yeeWorkAnswer.getId()));
                        map.put("recordId", String.valueOf(yeeWorkAnswer.getRecordId()));
                        map.put("examId", String.valueOf(yeeWorkAnswer.getExamId()));
                        map.put("userId", String.valueOf(yeeWorkAnswer.getUserId()));
                        map.put("courseId", String.valueOf(yeeWorkAnswer.getCourseId()));
                        map.put("topicId", String.valueOf(yeeWorkAnswer.getTopicId()));
                        map.put("answer", yeeWorkAnswer.getAnswer());
                        return map;
                    }).collect(Collectors.toList());

            // 计算出选择题的结果
            List<Map<String, Object>> calculateAnswerScores = ScoreCalculator.calculateAnswerScores(maps, yeeWorkAnswersMap);
            log.info("交卷-算分结果数量: {}", calculateAnswerScores.size());

            Map<Object, Object> resultMap = new HashMap<>();
            resultMap.put("calculateAnswerScores", calculateAnswerScores);
            resultMap.put("yeeWorkTopic", maps);

            // 判断maps中的题目是否包含type=4（主观题），有则 state=2 待批，无则 state=3
            final int state;
            boolean hasSubjective = false;
            for (Map<String, Object> map : maps) {
                Object typeObj = map.get("type");
                if (typeObj != null) {
                    Integer type = null;
                    if (typeObj instanceof Integer) {
                        type = (Integer) typeObj;
                    } else {
                        try {
                            type = Integer.parseInt(typeObj.toString());
                        } catch (NumberFormatException e) {
                            continue;
                        }
                    }
                    if (type == 4) {
                        hasSubjective = true;
                        break;
                    }
                }
            }
            state = hasSubjective ? 2 : 3;
            log.info("交卷-状态: state={} ({}主观题)", state, hasSubjective ? "含" : "无");

            // 4. 事务内执行所有写操作，确保原子性
            databaseUtil.executeInTransaction(schoolId, conn -> {
                try {
                    updateAnswerScores(conn, schoolId, calculateAnswerScores);
                    // 改动点：新增最后一个参数 submitType=1 代表学生手动交卷
                    updateWorkRecordFinishState(conn, schoolId, recordId, workId, userId, courseId, calculateAnswerScores, state, 1);
                    updateExamCountForCourse(conn, schoolId, courseId, userId);
                    updateYeeWorkScore(conn, workId, userId, courseId, schoolId, calculateAnswerScores, state);
                } catch (Exception e) {
                    log.error("交卷事务失败: schoolId={}, recordId={}", schoolId, recordId, e);
                    throw new RuntimeException(e);
                }
            });

            log.info("交卷完成: schoolId={}, recordId={}", schoolId, recordId);
            return Result.success(resultMap);
        } catch (Exception e) {
            log.error("交卷失败: schoolId={}, courseId={}, userId={}, examId={}, recordId={}",
                    schoolId, courseId, userId, workId, recordId, e);
            throw e;
        }
    }

    /**
    教师后台批量强制收卷接口
     */
    @Transactional
    @Override
    public Result teacherBatchCollectExam(int schoolId, Integer examId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        Connection mainConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        mainConn.setAutoCommit(false); // 批量统一事务

        int successCount = 0;
        List<Integer> failStudentIds = new ArrayList<>();
        Map<Integer, String> failMsgMap = new HashMap<>();
        List<Map<String, Integer>> allUnSubmitRecord = new ArrayList<>();

        try {
            // 第一步：查询本场试卷下所有未交卷学生记录 state=1
            String queryUnSubmitSql = """
                SELECT id AS recordId, userId, courseId
                FROM yee_exam_record
                WHERE schoolId = ? AND examId = ? AND state = 1
                """;
            PreparedStatement pstQuery = mainConn.prepareStatement(queryUnSubmitSql);
            pstQuery.setInt(1, schoolId);
            pstQuery.setInt(2, examId);
            ResultSet rs = pstQuery.executeQuery();
            while (rs.next()) {
                Map<String, Integer> item = new HashMap<>();
                item.put("recordId", rs.getInt("recordId"));
                item.put("userId", rs.getInt("userId"));
                item.put("courseId", rs.getInt("courseId"));
                allUnSubmitRecord.add(item);
            }
            closeResultSetAndStatement(rs, pstQuery);

            if (allUnSubmitRecord.isEmpty()) {
                return Result.success("本场试卷暂无未交卷学生，无需收卷");
            }

            // 循环处理每一条未交卷记录
            for (Map<String, Integer> item : allUnSubmitRecord) {
                Integer recordId = item.get("recordId");
                Integer userId = item.get("userId");
                Integer courseId = item.get("courseId");
                try {
                    // submitType=2 教师强制收卷，复用统一交卷事务逻辑
                    doFinishExamTransaction(mainConn, schoolId, recordId, examId, userId, courseId, 2);
                    successCount++;
                } catch (SQLException e) {
                    failStudentIds.add(userId);
                    failMsgMap.put(userId, e.getMessage());
                }
            }
            mainConn.commit();
        } catch (Exception e) {
            mainConn.rollback();
            log.error("教师按试卷批量收卷整体事务回滚 schoolId={},examId={}", schoolId, examId, e);
            throw new Exception("试卷批量收卷异常，全部操作已回滚：" + e.getMessage());
        } finally {
            mainConn.setAutoCommit(true);
            closeConnection(mainConn);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalUnSubmitNum", allUnSubmitRecord.size());
        result.put("successCount", successCount);
        result.put("failStudentIds", failStudentIds);
        result.put("failDetail", failMsgMap);
        if (!failStudentIds.isEmpty()) {
            result.put("tip", "部分学生收卷失败，详情查看failDetail");
        }
        return Result.success(result);
    }
    /**
     * 考试超时自动收卷（定时任务调用）
     * @return
     * @throws Exception
     */
    // 改为执行完成延迟10分钟再跑下一轮，杜绝并发重叠
    @Scheduled(fixedDelay = 600000)
    public void scheduleAutoTimeoutCollect() {
        log.info("【考试定时收卷】本轮全局扫描开始");
        try {
            List<Map<String, Integer>> allSchoolExamList = getAllSchoolExamList();
            if (allSchoolExamList.isEmpty()) {
                log.info("【考试定时收卷】无到期试卷，直接结束");
                return;
            }
            // 1、按学校分组，同一学校试卷聚合
            Map<Integer, List<Integer>> schoolExamGroup = new HashMap<>();
            for (Map<String, Integer> seMap : allSchoolExamList) {
                Integer schoolId = seMap.get("schoolId");
                Integer examId = seMap.get("examId");
                if (schoolId == null || examId == null) continue;
                schoolExamGroup.computeIfAbsent(schoolId, k -> new ArrayList<>()).add(examId);
            }
            List<Integer> schoolIdList = new ArrayList<>(schoolExamGroup.keySet());
            // 2、逐校串行执行，校间间隔5分钟
            for (int i = 0; i < schoolIdList.size(); i++) {
                Integer schoolId = schoolIdList.get(i);
                List<Integer> examIdList = schoolExamGroup.get(schoolId);
                log.info("【考试定时收卷】开始处理schoolId={}，待处理试卷数：{}", schoolId, examIdList.size());
                SlSchool slSchool = slSchoolMapper.selectById(schoolId);
                for (Integer examId : examIdList) {
                    try {
                        autoTimeoutCollectExam(schoolId, examId, slSchool);
                    } catch (Exception e) {
                        log.error("【定时任务】试卷监控失败 schoolId={},examId={}", schoolId, examId, e);
                    }
                }

            }
        } catch (Exception e) {
            log.error("【定时任务】全局扫描异常，流程中断", e);
        }
    }

    /**
     * 重载：同一张试卷共用1条事务连接，不再每个学生新建连接
     */
    public Result autoTimeoutCollectExam(int schoolId, Integer examId, SlSchool slSchool) throws Exception {
        long nowSec = System.currentTimeMillis() / 1000;
        Connection queryConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        List<Map<String, Integer>> timeoutRecordList = new ArrayList<>();

        try {
            String queryTimeoutSql = """
        SELECT r.id AS recordId, r.userId, r.courseId
        FROM yee_exam_record r
        INNER JOIN yee_exam e ON r.examId = e.id AND r.schoolId = e.schoolId
        WHERE r.schoolId = ? AND r.examId = ? AND r.state = 1 AND ? > e.endTime
        """;
            PreparedStatement pstQuery = queryConn.prepareStatement(queryTimeoutSql);
            pstQuery.setInt(1, schoolId);
            pstQuery.setInt(2, examId);
            pstQuery.setLong(3, nowSec);
            ResultSet rs = pstQuery.executeQuery();
            while (rs.next()) {
                Map<String, Integer> item = new HashMap<>();
                item.put("recordId", rs.getInt("recordId"));
                item.put("userId", rs.getInt("userId"));
                item.put("courseId", rs.getInt("courseId"));
                timeoutRecordList.add(item);
            }
            closeResultSetAndStatement(rs, pstQuery);
        } finally {
            closeConnection(queryConn);
        }

        if (timeoutRecordList.isEmpty()) {
            return Result.success("暂无超时未交卷学生");
        }

        int successCount = 0;
        List<Integer> failUserIds = new ArrayList<>();
        Map<Integer, String> failMsg = new HashMap<>();

        // 整张试卷仅1个连接，节约数据库连接资源
        Connection transConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        transConn.setAutoCommit(false);
        try {
            for (Map<String, Integer> item : timeoutRecordList) {
                Integer recordId = item.get("recordId");
                Integer userId = item.get("userId");
                Integer courseId = item.get("courseId");
                Savepoint sp = null;
                try {
                    // 增加前缀，规范保存点名
                    sp = transConn.setSavepoint("sp_record_" + recordId);
                    doFinishExamTransaction(transConn, schoolId, recordId, examId, userId, courseId, 3);
                    successCount++;
                } catch (Exception e) {
                    String errMsg = e.getMessage();
                    // 判空，防止空指针
                    if (errMsg != null && errMsg.contains("该学生已完成交卷，禁止重复操作")) {
                        log.warn("【自动收卷-并发跳过】schoolId={},examId={},recordId={},用户已手动交卷无需处理", schoolId, examId, recordId);
                        continue;
                    }
                    // 本条异常，局部回滚
                    if (sp != null) {
                        transConn.rollback(sp);
                    }
                    failUserIds.add(userId);
                    String errLog = "recordId:" + recordId + ",err:" + errMsg;
                    failMsg.put(userId, errLog);
                    log.error("【考试单学生收卷失败】schoolId={},examId={},userId={},msg={}", schoolId, examId, userId, errLog, e);
                } finally {
                    // 必释放保存点，避免事务内存堆积
                    if (sp != null) {
                        transConn.releaseSavepoint(sp);
                    }
                }
            }
            // 循环全部执行完毕，统一提交所有正常学生数据
            transConn.commit();
            log.info("【自动收卷批量提交完成】schoolId={},examId={},总待处理:{},成功:{},失败:{}",
                    schoolId, examId, timeoutRecordList.size(), successCount, failUserIds.size());
        } catch (Exception e) {
            // 仅连接中断、数据库宕机等极端全局异常才整体回滚
            transConn.rollback();
            log.error("【试卷批量收卷全局异常，整体事务回滚 schoolId={},examId={}", schoolId, examId, e);
        } finally {
            transConn.setAutoCommit(true);
            closeConnection(transConn);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("totalTimeoutNum", timeoutRecordList.size());
        res.put("successNum", successCount);
        res.put("failUserIds", failUserIds);
        res.put("failDetail", failMsg);
        return Result.success(res);
    }

    // 兼容原有外部调用，上层代码无需改动
    public Result autoTimeoutCollectExam(int schoolId, Integer examId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        return autoTimeoutCollectExam(schoolId, examId, slSchool);
    }

    /**
     * 查询【仅启用allow=1】学校 + 10分钟内到期试卷，过滤禁用院校
     */
    private List<Map<String, Integer>> getAllSchoolExamList() throws Exception {
        List<Map<String, Integer>> finalList = new ArrayList<>();
        long nowSec = System.currentTimeMillis() / 1000;
        long monitorMaxTime = nowSec;

        LambdaQueryWrapper<SlSchool> schoolWrapper = new LambdaQueryWrapper<>();
        schoolWrapper.eq(SlSchool::getAllow, 1);
        List<SlSchool> openSchoolList = slSchoolMapper.selectList(schoolWrapper);
        log.info("【定时任务】已启用学校总数：{}", openSchoolList.size());

        for (SlSchool school : openSchoolList) {
            Integer schoolId = school.getId();
            Connection conn = SlaveMysqlConnectionUtil.getConnection(school);
            String sql = """
        SELECT DISTINCT id AS examId 
        FROM yee_exam 
        WHERE schoolId = ? AND endTime <= ?
        """;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, schoolId);
            pst.setLong(2, monitorMaxTime);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Map<String, Integer> map = new HashMap<>();
                map.put("schoolId", schoolId);
                map.put("examId", rs.getInt("examId"));
                finalList.add(map);
            }
            closeResultSetAndStatement(rs, pst);
            closeConnection(conn);
        }
        return finalList;
    }
    /**
     * 更新考试记录交卷状态，统一适配：学生手动交卷/教师收卷/自动超时收卷
     * @param conn 数据库连接
     * @param schoolId 学校ID
     * @param recordId 答题记录主键
     * @param examId 考试ID
     * @param userId 学生ID
     * @param courseId 课程ID
     * @param scoredResults 客观题得分明细
     * @param state 最终状态 2待批阅 /3已批阅
     * @param submitType 交卷类型 1学生手动 2教师强制 3系统超时
     */
    private void updateWorkRecordFinishState(Connection conn, int schoolId, Integer recordId, Integer examId, Integer userId, Integer courseId, List<Map<String, Object>> scoredResults, int state, int submitType) throws Exception {
        PreparedStatement st = null;

        // 计算客观总分
        BigDecimal totalEarned = scoredResults.stream()
                .map(item -> (BigDecimal) item.get("earnedScore"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            // 限制总分不超过试卷满分
            try {
                YeeExam exam = queryExamById(conn, examId);
                if (exam != null && exam.getScore() != null && exam.getScore() > 0) {
                    BigDecimal maxScore = BigDecimal.valueOf(exam.getScore());
                    if (totalEarned.compareTo(maxScore) > 0) {
                        totalEarned = maxScore;
                    }
                }
            } catch (Exception e) {
                // 查询试卷信息失败不阻断更新流程
            }

            long timeSec = System.currentTimeMillis() / 1000;
            // 新增 submitType、submitTime 字段；WHERE增加 state=1 幂等控制
            String sql = """
            UPDATE yee_exam_record
            SET state = ?,
                finishTime = ?,
                score = ?,
                markTime = ?,
                obScore = ?,
                submitType = ?,
                submitTime = ?
            WHERE id = ?
              AND examId = ?
              AND userId = ?
              AND courseId = ?
              AND schoolId = ?
              AND state = 1
            """;

            st = conn.prepareStatement(sql);
            int paramIndex = 1;
            st.setInt(paramIndex++, state);
            st.setObject(paramIndex++, timeSec);    // finishTime
            st.setObject(paramIndex++, totalEarned);// score总分
            st.setObject(paramIndex++, timeSec);    // markTime
            st.setObject(paramIndex++, totalEarned);// obScore客观分
            st.setInt(paramIndex++, submitType);    // 新增：交卷类型 1/2/3
            st.setObject(paramIndex++, timeSec);    // 新增：提交时间戳
            st.setInt(paramIndex++, recordId);
            st.setInt(paramIndex++, examId);
            st.setInt(paramIndex++, userId);
            st.setInt(paramIndex++, courseId);
            st.setInt(paramIndex, schoolId);

            int rowsAffected = st.executeUpdate();
            // 无更新行数=记录已交卷(state≠1)，抛出异常阻断流程
            if (rowsAffected == 0) {
                throw new Exception("更新考试记录失败：该学生已完成交卷，禁止重复操作。" +
                        "recordId=" + recordId + ", examId=" + examId +
                        ", userId=" + userId + ", schoolId=" + schoolId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("更新 yee_exam_record 状态失败，recordId=" + recordId + ", examId=" + examId, e);
        } finally {
            closeResultSetAndStatement(null, st);
        }
    }

    private void updateAnswerScores(Connection conn, int schoolId, List<Map<String, Object>> scoredResults) throws Exception {
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE yee_exam_answer SET score = ?, marked = 1, hit = ? " +
                    "WHERE recordId = ? AND examId = ? AND topicId = ? AND userId = ? AND courseId = ? AND schoolId = ?";
            ps = conn.prepareStatement(sql);
            for (Map<String, Object> item : scoredResults) {
                ps.setBigDecimal(1, getBigDecimal(item, "earnedScore"));
                ps.setInt(2, getInteger(item, "correctStatus"));
                ps.setInt(3, getInteger(item, "recordId"));
                ps.setInt(4, getInteger(item, "examId"));
                ps.setInt(5, getInteger(item, "topicId"));
                ps.setInt(6, getInteger(item, "userId"));
                ps.setInt(7, getInteger(item, "courseId"));
                ps.setInt(8, schoolId);
                ps.addBatch();
            }
            ps.executeBatch();
        } finally {
            if (ps != null) ps.close();
        }
    }

    /**
     * 根据条件查询 yee_work_answer 记录
     *
     * @param schoolId  学校ID
     * @param recordId  记录ID
     * @param examId    作业ID
     * @param userId    用户ID
     * @param courseId  课程ID
     * @return 查询结果列表
     * @throws Exception 查询失败
     */
    private List<YeeExamAnswer> queryYeeWorkAnswers(Integer schoolId, Integer recordId,
                                                    Integer examId, Integer userId,
                                                    Integer courseId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<YeeExamAnswer> resultList = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 2. 获取主库连接（读操作也可用主库，保持一致性）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. SQL 查询语句
            String sql = """
            SELECT *
            FROM yee_exam_answer
            WHERE schoolId = ? 
              AND recordId = ?
              AND examId = ?
              AND userId = ?
              AND courseId = ?
            """;

            st = conn.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, recordId);
            st.setInt(3, examId);
            st.setInt(4, userId);
            st.setInt(5, courseId);

            // 4. 执行查询
            rs = st.executeQuery();

            // 5. 封装结果
            ObjectMapper objectMapper = new ObjectMapper(); // 用于 JSON 反序列化
            while (rs.next()) {
                YeeExamAnswer answer = new YeeExamAnswer();
                answer.setId(rs.getInt("id"));
                answer.setRecordId(rs.getInt("recordId"));
                answer.setExamId(rs.getInt("examId"));
                answer.setTopicId(rs.getInt("topicId"));
                answer.setAnswered(rs.getInt("answered"));
                answer.setScore(rs.getBigDecimal("score"));
                answer.setAnswer(rs.getString("answer"));
                answer.setMarked(rs.getString("marked"));
                answer.setRemark(rs.getString("remark"));
                answer.setHit(rs.getInt("hit"));
                answer.setUserId(rs.getInt("userId"));
                answer.setCourseId(rs.getInt("courseId"));
                answer.setSchoolId(rs.getInt("schoolId"));

                resultList.add(answer);
            }

            return resultList;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询 yee_work_answer 失败，条件: schoolId=" + schoolId +
                    ", recordId=" + recordId +
                    ", examId=" + examId +
                    ", userId=" + userId +
                    ", courseId=" + courseId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 根据 YeeWorkAnswer 对象更新指定记录的 answered 和 answer 字段
     *
     * @param answer
     * @return 是否更新成功
     * @throws Exception 更新失败时抛出异常
     */
    private boolean updateYeeWorkAnswer(Integer schoolId, Integer courseId, Integer userId, List<String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception {
        int retry = 0;
        while (retry < 3) {
            Connection conn = null;
            PreparedStatement st = null;
            String answerJson = null;
            try {
                if (answer == null || answer.isEmpty()) {
                    answerJson = "";
                } else if(type == 2) {
                    answerJson = new ObjectMapper().writeValueAsString(answer);
                } else {
                    answerJson = answer.get(0);
                }
                SlSchool slSchool = slSchoolMapper.selectById(schoolId);
                if (slSchool == null || slSchool.getAllow() == 0) {
                    throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
                }
                conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
                String sql = "UPDATE yee_exam_answer SET answered=1, answer=? WHERE recordId=? AND examId=? AND topicId=? AND userId=? AND courseId=? AND schoolId=?";
                st = conn.prepareStatement(sql);
                st.setString(1, answerJson);
                st.setInt(2, recordId);
                st.setInt(3, workId);
                st.setInt(4, topicId);
                st.setInt(5, userId);
                st.setInt(6, courseId);
                st.setInt(7, schoolId);
                int rows = st.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                retry++;
                if (retry >= 3 || (!e.getMessage().contains("Deadlock found") && !e.getMessage().contains("lock wait"))) {
                    log.error("更新yee_exam_answer失败: schoolId={}, recordId={}, examId={}, topicId={}, userId={}",
                            schoolId, recordId, workId, topicId, userId, e);
                    throw new Exception("更新失败", e);
                }
                log.warn("更新yee_exam_answer遇到死锁, 第{}次重试: schoolId={}, recordId={}, topicId={}",
                        retry, schoolId, recordId, topicId);
                Thread.sleep(100);
            } finally {
                if (st != null) st.close();
                if (conn != null) conn.close();
            }
        }
        return false;
    }


    /**
     * 根据 YeeWorkAnswer 对象更新指定记录的 answered 和 answer 字段
     *
     * @param answer
     * @return 是否更新成功
     * @throws Exception 更新失败时抛出异常
     */
    private boolean updateYeeWorkAnswerText(Integer schoolId, Integer courseId, Integer userId, String answer, Integer topicId, Integer workId, Integer recordId, Integer type, List<FileInfo> images, List<FileInfo> files) throws Exception {
        int retry = 0;
        while (retry < 3) {
            Connection conn = null;
            PreparedStatement st = null;
            try {
                SlSchool slSchool = slSchoolMapper.selectById(schoolId);
                if (slSchool == null || slSchool.getAllow() == 0) {
                    throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
                }
                conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
                String imagesStr = new ObjectMapper().writeValueAsString(images);
                String filesStr = new ObjectMapper().writeValueAsString(files);
                String sql = "UPDATE yee_exam_answer SET answered=1, answer=?, images=?, files=? WHERE recordId=? AND examId=? AND topicId=? AND userId=? AND courseId=? AND schoolId=?";
                st = conn.prepareStatement(sql);
                st.setString(1, answer);
                st.setString(2, imagesStr);
                st.setString(3, filesStr);
                st.setInt(4, recordId);
                st.setInt(5, workId);
                st.setInt(6, topicId);
                st.setInt(7, userId);
                st.setInt(8, courseId);
                st.setInt(9, schoolId);
                int rows = st.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                retry++;
                if (retry >= 3 || (!e.getMessage().contains("Deadlock found") && !e.getMessage().contains("lock wait"))) {
                    log.error("更新yee_exam_answer(主观题)失败: schoolId={}, recordId={}, examId={}, topicId={}, userId={}",
                            schoolId, recordId, workId, topicId, userId, e);
                    throw new Exception("更新失败", e);
                }
                log.warn("更新yee_exam_answer(主观题)遇到死锁, 第{}次重试: schoolId={}, recordId={}, topicId={}",
                        retry, schoolId, recordId, topicId);
                Thread.sleep(100);
            } finally {
                if (st != null) st.close();
                if (conn != null) conn.close();
            }
        }
        return false;
    }


    /**
     * 根据 YeeWorkAnswer 对象更新指定记录的 answered 和 answer 字段
     *
     * @param answer
     * @return 是否更新成功
     * @throws Exception 更新失败时抛出异常
     */
    private boolean updateYeeWorkAnswerBlank(Integer schoolId, Integer courseId, Integer userId, Map<String, String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception {
        int retry = 0;
        while (retry < 3) {
            Connection conn = null;
            PreparedStatement st = null;
            try {
                SlSchool slSchool = slSchoolMapper.selectById(schoolId);
                if (slSchool == null || slSchool.getAllow() == 0) {
                    throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
                }
                conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
                String answerStr = new ObjectMapper().writeValueAsString(answer);
                String sql = "UPDATE yee_exam_answer SET answered=1, answer=? WHERE recordId=? AND examId=? AND topicId=? AND userId=? AND courseId=? AND schoolId=?";
                st = conn.prepareStatement(sql);
                st.setString(1, answerStr);
                st.setInt(2, recordId);
                st.setInt(3, workId);
                st.setInt(4, topicId);
                st.setInt(5, userId);
                st.setInt(6, courseId);
                st.setInt(7, schoolId);
                int rows = st.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                retry++;
                if (retry >= 3 || (!e.getMessage().contains("Deadlock found") && !e.getMessage().contains("lock wait"))) {
                    log.error("更新yee_exam_answer(填空题)失败: schoolId={}, recordId={}, examId={}, topicId={}, userId={}",
                            schoolId, recordId, workId, topicId, userId, e);
                    throw new Exception("更新失败", e);
                }
                log.warn("更新yee_exam_answer(填空题)遇到死锁, 第{}次重试: schoolId={}, recordId={}, topicId={}",
                        retry, schoolId, recordId, topicId);
                Thread.sleep(100);
            } finally {
                if (st != null) st.close();
                if (conn != null) conn.close();
            }
        }
        return false;
    }




    /**
     * 批量插入作业答题记录
     *
     * @param yeeWorkAnswers 答题记录列表
     * @return 是否全部插入成功
     * @throws Exception 插入失败
     */
    private boolean insertYeeWorkAnswers(List<YeeExamAnswer> yeeWorkAnswers) throws Exception {
        if (yeeWorkAnswers == null || yeeWorkAnswers.isEmpty()) {
            return true; // 空列表视为成功
        }

        Connection conn = null;
        PreparedStatement st = null;

        try {
            // 1. 取第一条记录的 schoolId 用于获取数据源
            Integer schoolId = yeeWorkAnswers.get(0).getSchoolId();

            // 2. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 3. 获取主库连接（写操作）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 4. SQL 插入语句（排除自增 id 和 addDate 虚拟列）
            String sql = """
            INSERT INTO yee_exam_answer 
                (recordId, examId, topicId, answered, score, 
                 answer, images, files, marked, hit, userId, 
                 courseId, schoolId)
            VALUES 
                (?, ?, ?, ?, ?, ?, 
                 ?, ?, ?, ?, ?, ?, 
                 ?)
            """;

            st = conn.prepareStatement(sql);

            // 5. 遍历列表，设置参数并添加到批处理
            for (YeeExamAnswer answer : yeeWorkAnswers) {
                st.setInt(1,  answer.getRecordId());
                st.setInt(2,  answer.getExamId());
                st.setInt(3,  answer.getTopicId());
                st.setInt(4,  answer.getAnswered());
                st.setBigDecimal(5, answer.getScore());
                st.setString(6, answer.getAnswer());
                st.setString(7, answer.getImages());
                st.setString(8, answer.getFiles());
                st.setString(9, answer.getMarked());
                st.setInt(10, answer.getHit());
                st.setInt(11, answer.getUserId());
                st.setInt(12, answer.getCourseId());
                st.setInt(13, answer.getSchoolId());

                st.addBatch(); // 添加到批处理
            }

            // 6. 执行批处理
            int[] results = st.executeBatch();

            // 7. 检查批处理结果：只要不是 EXECUTE_FAILED，都视为成功
            // 注意：SUCCESS_NO_INFO (-2) 是合法的成功状态！
            for (int rows : results) {
                if (rows == Statement.EXECUTE_FAILED) {
                    return false; // 理论上很少进入这里，因失败通常抛异常
                }
                // 其他情况（>=0 或 -2）均视为成功
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            // 记录关键上下文
            String examId = yeeWorkAnswers.isEmpty() ? "unknown" : String.valueOf(yeeWorkAnswers.get(0).getExamId());
            Integer userId = yeeWorkAnswers.isEmpty() ? -1 : yeeWorkAnswers.get(0).getUserId();
            throw new Exception("批量插入 yee_exam_answer 失败，examId=" + examId +
                    ", userId=" + userId +
                    ", size=" + yeeWorkAnswers.size(), e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

    /**
     * 根据 paperId 查询试卷题目列表，返回 Map 列表（不使用实体类）
     *
     * @param schoolId 学校ID
     * @param workId  作业ID
     * @return 查询结果，每行是一个 Map<String, Object>
     * @throws Exception 查询失败
     */
    private List<Map<String, Object>> queryPaperTopicsAsMap(Integer schoolId, Integer workId, Integer userId, Integer recordId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 修复：仅查询试卷原始题目，删除无关record关联
            String sql = """
                SELECT wt.id, wt.number, wt.type, wt.score, wt.topic, wt.option, wt.examId
                FROM yee_exam_topic wt
                WHERE wt.examId = ?
                """;
            st = conn.prepareStatement(sql);
            st.setInt(1, workId);
            rs = st.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            ObjectMapper objectMapper = OBJECT_MAPPER; // 使用全局静态实例
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    if ("option".equalsIgnoreCase(columnName) && value != null) {
                        try {
                            value = objectMapper.readValue(value.toString(), List.class);
                        } catch (Exception e) {}
                    }
                    row.put(columnName, value);
                    // 固定recordId为传入值，兼容上层逻辑
                    row.put("recordId", recordId);
                }
                result.add(row);
            }
            return result;
        } catch (Exception e) {
            throw new Exception("查询试卷题目失败，workId=" + workId + ", schoolId=" + schoolId, e);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }
    /**
     * 【交卷专用】
     * 根据 recordId 查询学生【实际抽到的题目】
     * 来源：yee_exam_answer
     * 随机/不随机 都通用
     */
    private List<Map<String, Object>> queryStudentExamTopicsByRecordId(Integer schoolId, Integer recordId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // ✅ 核心：只查学生本次考试抽到的题目
            String sql = """
            SELECT wt.id,
                   wt.number,
                   wt.type,
                   wt.score,
                   wt.topic,
                   wt.option,
                   wt.examId
            FROM yee_exam_answer wa
            INNER JOIN yee_exam_topic wt ON wa.topicId = wt.id
            WHERE wa.recordId = ?
        """;

            st = conn.prepareStatement(sql);
            st.setInt(1, recordId);

            rs = st.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            ObjectMapper objectMapper = new ObjectMapper();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);

                    if ("option".equalsIgnoreCase(columnName) && value != null) {
                        try {
                            value = objectMapper.readValue(value.toString(), List.class);
                        } catch (Exception e) {}
                    }
                    row.put(columnName, value);
                }
                result.add(row);
            }
            return result;

        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 插入一条考试记录，并返回自增主键 id
     *
     * @param record YeeExamRecord 对象
     * @return 自增主键 id（> 0 表示成功）
     * @throws Exception 插入失败或获取 ID 失败
     */
    private int insertYeeExamRecord(YeeExamRecord record) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        int generatedId = -1;

        try {
            Integer schoolId = record.getSchoolId();

            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 2. 获取主库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. SQL 插入语句：新增 selectTopicIds 字段
            String sql = """
        INSERT INTO yee_exam_record 
            (examId, userId, startTime, state, finishTime, score, 
             isCancel, frequency, teacherId, markTime, obScore, subScore, 
             markOrder, platform, courseId, classId, schoolId,
             submitType, submitTime, lastActiveTime, selectTopicIds)
        VALUES 
            (?, ?, ?, ?, ?, ?, 
             ?, ?, ?, ?, ?, ?, 
             ?, ?, ?, ?, ?,
             ?, ?, ?, ?)
        """;

            st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            st.setInt(1,  record.getExamId());
            st.setInt(2,  record.getUserid());
            st.setInt(3,  record.getStartTime());
            st.setInt(4,  record.getState());
            st.setInt(5,  record.getFinishTime());
            st.setBigDecimal(6, record.getScore());
            st.setInt(7,  record.getIsCancel());
            st.setInt(8,  record.getFrequency());
            st.setInt(9,  record.getTeacherId());
            st.setInt(10, record.getMarkTime());
            st.setBigDecimal(11, record.getObScore());
            st.setBigDecimal(12, record.getSubScore());
            st.setInt(13, record.getMarkOrder());
            st.setString(14, record.getPlatform());
            st.setInt(15, record.getCourseId());
            st.setInt(16, record.getClassId());
            st.setInt(17, record.getSchoolId());

            // 3个扩展字段
            st.setInt(18, record.getSubmitType());
            st.setInt(19, record.getSubmitTime());
            st.setInt(20, record.getLastActiveTime());

            // 新增：selectTopicIds 赋值，第21个占位符
            st.setString(21, record.getSelectTopicIds());

            // 5. 执行插入
            int rowsAffected = st.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("插入 yee_exam_record 失败，影响行数为 0");
            }

            // 6. 获取自增主键
            rs = st.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            } else {
                throw new Exception("未能获取自增主键");
            }

            return generatedId;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("插入 yee_exam_record 失败，examId=" + record.getExamId() +
                    ", userId=" + record.getUserid() +
                    ", schoolId=" + record.getSchoolId(), e);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getStudentExamInfoByCourseAndStudent(
            Integer schoolId,
            Integer courseId,
            Integer studentId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL 查询语句
            String sql = """
                SELECT 
                    cs.studentId,
                    s.name AS studentName,
                    s.number,
                    c.name AS className,
                    w.score,
                    w.title,
                    w.startTime,
                    w.endTime,
                    w.paperId,
                    w.id,
                    w.createUserId,
                    w.random,
                    w.randData,
                    w.randNumber,
                    w.limitedTime,
                    cs.classId
                FROM 
                    yee_course_student cs
                    LEFT JOIN yee_student s ON s.id = cs.studentId
                    LEFT JOIN yee_course_class c ON c.id = cs.classId
                    LEFT JOIN yee_exam w ON w.courseId = cs.courseId AND ( JSON_LENGTH(w.classList) = 0 OR JSON_CONTAINS(w.classList, CAST(cs.classId AS JSON)))
                WHERE 
                    cs.courseId = ?
                    AND cs.studentId = ?
                    AND w.allow = 1
                    AND w.schoolId = ?
                ORDER BY 
                    w.sequence,
                    w.addTime DESC
                """;

            st = conn.prepareStatement(sql);

            // 4. 设置参数
            st.setLong(1, courseId);
            st.setLong(2, studentId);
            st.setInt(3, schoolId);

            // 5. 执行查询
            rs = st.executeQuery();

            // 6. 封装结果集
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 数据库是时间戳 需要转换成时间
            // 创建自定义格式器
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    if ("startTime".equals(columnName) || "endTime".equals(columnName)){
                        long TimeSeconds = rs.getLong(columnName);
                        if (TimeSeconds == 0) {
                            row.put(columnName, rs.getObject(i));
                        } else {
                            row.put(columnName, LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(TimeSeconds),
                                    ZoneId.systemDefault()
                            ).format(formatter)); // 调用 .format() 转为字符串
                        }
                    } else if ("randData".equals(columnName)){
                        Map<String, Integer> randData = JSON.parseObject(rs.getString("randData"), new TypeReference<Map<String, Integer>>() {
                        });
                        row.put(columnName, randData);
                    } else {
                        row.put(columnName, rs.getObject(i));
                    }
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询学生作业信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId +
                    ", studentId=" + studentId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private Integer getExamFrequencyByUserAndWork(
            Integer schoolId,
            Integer userId,
            Integer workId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        Integer frequency = 0; // 默认返回 null

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL（精确查询 frequency）
            String sql = """
                SELECT 
                    wr.frequency
                FROM 
                    yee_exam_record wr
                WHERE 
                    wr.userId = ?
                    AND wr.examId = ?
                """;

            st = conn.prepareStatement(sql);

            // 4. 设置参数
            st.setLong(1, userId);
            st.setLong(2, workId);

            // 5. 执行查询
            rs = st.executeQuery();
            if (rs.next()) {
                frequency = rs.getInt("frequency");
            } else {
                frequency = 0; // 没有找到匹配的记录，设置默认值
            }

            return frequency;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询考试记录 frequency 失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", examId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getExamInfoByCourseAndStudent(
            Integer schoolId,
            Integer courseId,
            Integer studentId,
            Integer nodeId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建动态 SQL（支持 nodeId 可选条件）
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
                SELECT 
                    cs.courseId,
                    cs.studentId,
                    w.id AS examId,
                    w.title,
                    w.startTime,
                    w.endTime,
                    w.score,
                    w.nodeId
                FROM 
                    yee_course_student cs
                    LEFT JOIN yee_exam w ON w.courseId = cs.courseId  AND ( JSON_LENGTH(w.classList) = 0 OR JSON_CONTAINS(w.classList, CAST(cs.classId AS JSON)))
                WHERE 
                    cs.courseId = ?
                    AND cs.studentId = ?
                    AND w.allow = 1
                    AND w.schoolId = ?
                """);

            // 条件：nodeId 可选
            if (nodeId != null) {
                sqlBuilder.append(" AND w.nodeId = ? ");
            }

            sqlBuilder.append("""
                ORDER BY 
                    w.sequence,
                    w.addTime DESC
                """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, courseId);
            st.setLong(paramIndex++, studentId);
            st.setInt(paramIndex++, schoolId);

            if (nodeId != null) {
                st.setInt(paramIndex++, nodeId);
            }

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 数据库是时间戳 需要转换成时间
            // 创建自定义格式器
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    if ("startTime".equals(columnName) || "endTime".equals(columnName)){
                        long TimeSeconds = rs.getLong(columnName);
                        if (TimeSeconds == 0) {
                            row.put(columnName, rs.getObject(i));
                        } else {
                            row.put(columnName, LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(TimeSeconds),
                                    ZoneId.systemDefault()
                            ).format(formatter)); // 调用 .format() 转为字符串
                        }
                    } else {
                        row.put(columnName, rs.getObject(i));
                    }
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询学生作业信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId +
                    ", studentId=" + studentId +
                    ", nodeId=" + nodeId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    // ---------------- 工具方法 ----------------

    /**
     * 安全关闭 ResultSet 和 PreparedStatement
     */
    private void closeResultSetAndStatement(ResultSet rs, PreparedStatement stmt) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace(); // 建议使用日志框架
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 安全关闭 Connection
     */
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        } else if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        } else if (val != null) {
            try {
                String str = val.toString().trim();
                if (str.isBlank()) {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                // 格式错误兜底0
                return BigDecimal.ZERO;
            }
        }
        // val为null直接返回0，不再返回null
        return BigDecimal.ZERO;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val != null) {
            try {
                String str = val.toString().trim();
                if (str.isBlank()) {
                    return 0;
                }
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                // 字符串转数字失败，兜底0
                return 0;
            }
        }
        // val 为 null 直接返回0，不再返回null
        return 0;
    }



    /**
     * 更新课程学生表中的考试数量
     * @param schoolId 学校ID
     * @param courseId 课程ID
     * @param userId 用户ID
     * @throws Exception
     */
    private void updateExamCountForCourse(int schoolId, Integer courseId, Integer userId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 查询该学生在该课程中的考试数量（已完成的考试记录数）
            String countExamSql = "SELECT COUNT(*) as examCount FROM yee_exam_record WHERE courseId = ? AND userId = ? AND (state = 3 or state = 2)";
            st = conn.prepareStatement(countExamSql);
            st.setInt(1, courseId);
            st.setInt(2, userId);
            rs = st.executeQuery();

            int examCount = 0;
            if (rs.next()) {
                examCount = rs.getInt("examCount");
            }

            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 4. 更新 yee_course_student 表中的 examCount 字段
            String updateSql = "UPDATE yee_course_student SET examLearned = ? WHERE courseId = ? AND studentId = ?";
            st = conn.prepareStatement(updateSql);
            st.setInt(1, examCount);
            st.setInt(2, courseId);
            st.setInt(3, userId);
            int updateRows = st.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private void updateExamCountForCourse(Connection conn, int schoolId, Integer courseId, Integer userId) throws Exception {
        try {
            long examCount = databaseUtil.executeScalar(conn,
                    "SELECT COUNT(*) FROM yee_exam_record WHERE courseId = ? AND userId = ? AND (state = 3 OR state = 2)",
                    courseId, userId);

            databaseUtil.executeUpdate(conn, BuiltSql.of(
                    "UPDATE yee_course_student SET examLearned = ? WHERE courseId = ? AND studentId = ?",
                    (int) examCount, courseId, userId));
        } catch (Exception e) {
            throw new Exception("更新 yee_course_student examLearned 失败，courseId=" + courseId + ", userId=" + userId, e);
        }
    }

    private void updateYeeWorkScore(Integer workId, Integer userId, Integer courseId, Integer schoolId, List<Map<String, Object>> calculateAnswerScores, int state) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 2. 获取主库连接（写操作）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 计算总分
            BigDecimal totalEarned = calculateAnswerScores.stream()
                    .map(item -> (BigDecimal) item.get("earnedScore"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. 构建更新 SQL
            String sql = """
                UPDATE yee_exam_score 
                SET state = ?, 
                    scored = 1, 
                    submitTime = ?, 
                    finalScore = ?,
                    timeCost = (SELECT finishTime - startTime FROM yee_exam_record WHERE examId = ? AND userId = ? LIMIT 1)
                WHERE examId = ? 
                  AND userId = ?
                  AND schoolId = ?
                """;

            st = conn.prepareStatement(sql);

            // 5. 设置参数
            // state - 动态设置状态值
            st.setInt(1, state);
            // submitTime - 当前时间戳
            st.setInt(2, (int) (System.currentTimeMillis() / 1000));
            // finalScore
            st.setBigDecimal(3, totalEarned);
            // timeCost 参数中的 workId 和 userId
            st.setInt(4, workId);
            st.setInt(5, userId);
            // WHERE 子句参数
            st.setInt(6, workId);
            st.setInt(7, userId);
            st.setInt(8, schoolId);

            // 6. 执行更新
            int rowsAffected = st.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("更新 yee_exam_score 失败，examId=" + workId + ", userId=" + userId + ", courseId=" + courseId + ", schoolId=" + schoolId, e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

    private void updateYeeWorkScore(Connection conn, Integer workId, Integer userId, Integer courseId, Integer schoolId, List<Map<String, Object>> calculateAnswerScores, int state) throws Exception {
        PreparedStatement st = null;

        try {
            BigDecimal totalEarned = calculateAnswerScores.stream()
                    .map(item -> (BigDecimal) item.get("earnedScore"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String sql = """
                UPDATE yee_exam_score
                SET state = ?,
                    scored = 1,
                    submitTime = ?,
                    finalScore = ?,
                    timeCost = (SELECT finishTime - startTime FROM yee_exam_record WHERE examId = ? AND userId = ? LIMIT 1)
                WHERE examId = ?
                  AND userId = ?
                  AND schoolId = ?
                """;

            st = conn.prepareStatement(sql);

            st.setInt(1, state);
            st.setInt(2, (int) (System.currentTimeMillis() / 1000));
            st.setBigDecimal(3, totalEarned);
            st.setInt(4, workId);
            st.setInt(5, userId);
            st.setInt(6, workId);
            st.setInt(7, userId);
            st.setInt(8, schoolId);

            st.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("更新 yee_exam_score 失败，examId=" + workId + ", userId=" + userId + ", courseId=" + courseId + ", schoolId=" + schoolId, e);
        } finally {
            closeStatement(st);
        }
    }

    private void insertYeeWorkScore(Integer workId, Integer userId, Integer courseId, Integer schoolId, String platform) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 2. 获取主库连接（写操作）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 检查是否已存在记录，避免重复插入（基于 workId 和 userId 的唯一约束）
            String checkSql = "SELECT COUNT(*) FROM yee_exam_score WHERE examId = ? AND userId = ?";
            st = conn.prepareStatement(checkSql);
            st.setInt(1, workId);
            st.setInt(2, userId);
            ResultSet rs = st.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            closeResultSetAndStatement(rs, st);

            if (count > 0) {
                // 记录已存在，无需插入
                return;
            }

            // 4. 构建插入 SQL
            String sql = """
                INSERT INTO yee_exam_score 
                (examId, userId, finalScore, state, scored, submitTime, timeCost, platform, courseId, schoolId)
                VALUES 
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            st = conn.prepareStatement(sql);

            // 5. 设置参数
            // workId
            st.setInt(1, workId);
            // userId
            st.setInt(2, userId);
            // finalScore - 初始为 0
            st.setBigDecimal(3, new BigDecimal("0.00"));
            // state - 初始为 1
            st.setInt(4, 1);
            // scored - 初始为 false (0)
            st.setBoolean(5, false);
            // submitTime - 当前时间戳
            st.setInt(6, 0);
            // timeCost - 初始为 0
            st.setInt(7, 0);
            // platform
            st.setString(8, platform);
            // courseId
            st.setInt(9, courseId);
            // schoolId
            st.setInt(10, schoolId);

            // 6. 执行插入
            int rowsAffected = st.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("插入 yee_exam_score 失败，workId=" + workId + ", userId=" + userId + ", courseId=" + courseId + ", schoolId=" + schoolId, e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

    private void calculateSingleTopicScore(
            int schoolId,
            Integer courseId,
            Integer userId,
            Integer examId,
            Integer recordId,
            Integer topicId) throws Exception {

        if (courseId == null || userId == null || examId == null || recordId == null || topicId == null) {
            log.warn("calculateSingleTopicScore参数不完整: schoolId={}, courseId={}, userId={}, examId={}, recordId={}, topicId={}",
                    schoolId, courseId, userId, examId, recordId, topicId);
            return;
        }

        Connection conn = null;
        try {
            conn = databaseUtil.getConnection(schoolId);

            // 查题目
            List<Map<String, Object>> singleTopic = querySingleExamTopic(conn, examId, topicId);
            if (singleTopic == null || singleTopic.isEmpty()) {
                return;
            }

            // 查答案
            YeeExamAnswer singleAnswer = querySingleExamAnswer(conn, schoolId, recordId, topicId);
            if (singleAnswer == null) {
                return;
            }

            // 如果该题已批改过，跳过 MQ 发送（防止重复累加分数）
            boolean alreadyMarked = "1".equals(singleAnswer.getMarked());

            // 封装答案
            List<Map<String, Object>> answerList = new ArrayList<>();
            Map<String, Object> ansMap = new HashMap<>();
            ansMap.put("topicId", singleAnswer.getTopicId());
            ansMap.put("answer", singleAnswer.getAnswer());
            ansMap.put("recordId", singleAnswer.getRecordId());
            ansMap.put("examId", singleAnswer.getExamId());
            ansMap.put("userId", singleAnswer.getUserId());
            ansMap.put("courseId", singleAnswer.getCourseId());
            answerList.add(ansMap);

            // 算分
            List<Map<String, Object>> scoreList = ScoreCalculator.calculateAnswerScores(singleTopic, answerList);
            if (scoreList == null || scoreList.isEmpty()) {
                return;
            }

            BigDecimal earnedScore = ScoreCalculator.extractEarnedScore(scoreList);

            // 更新单题得分
            updateAnswerScores(conn, schoolId, scoreList);

            if (!alreadyMarked) {
                ExamScoreMessage msg = new ExamScoreMessage(
                        schoolId, recordId, examId, topicId, userId, courseId, earnedScore);
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.ROUTING_KEY, msg);
                log.debug("发送MQ消息: schoolId={}, recordId={}, topicId={}, earnedScore={}",
                        schoolId, recordId, topicId, earnedScore);
            }
        } catch (Exception e) {
            log.error("单题算分异常: schoolId={}, recordId={}, topicId={}", schoolId, recordId, topicId, e);
            throw e;
        } finally {
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> querySingleExamTopic(Connection conn, Integer examId, Integer topicId) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String sql = "SELECT wt.id, wt.number, wt.type, wt.score, wt.topic, wt.option FROM yee_exam_topic wt WHERE wt.examId = ? AND wt.id = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, examId);
            st.setInt(2, topicId);
            rs = st.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    if ("option".equalsIgnoreCase(columnName) && value != null) {
                        try {
                            value = OBJECT_MAPPER.readValue(value.toString(), List.class);
                        } catch (Exception ignored) {}
                    }
                    row.put(columnName, value);
                }
                result.add(row);
            }
            return result;
        } finally {
            closeResultSetAndStatement(rs, st);
        }
    }

    private YeeExamAnswer querySingleExamAnswer(Connection conn, Integer schoolId, Integer recordId, Integer topicId) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM yee_exam_answer WHERE schoolId = ? AND recordId = ? AND topicId = ? LIMIT 1";
            st = conn.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, recordId);
            st.setInt(3, topicId);
            rs = st.executeQuery();

            if (rs.next()) {
                YeeExamAnswer answer = new YeeExamAnswer();
                answer.setId(rs.getInt("id"));
                answer.setRecordId(rs.getInt("recordId"));
                answer.setExamId(rs.getInt("examId"));
                answer.setTopicId(rs.getInt("topicId"));
                answer.setAnswered(rs.getInt("answered"));
                answer.setScore(rs.getBigDecimal("score"));
                answer.setAnswer(rs.getString("answer"));
                answer.setMarked(rs.getString("marked"));
                answer.setRemark(rs.getString("remark"));
                answer.setHit(rs.getInt("hit"));
                answer.setUserId(rs.getInt("userId"));
                answer.setCourseId(rs.getInt("courseId"));
                answer.setSchoolId(rs.getInt("schoolId"));
                return answer;
            }
            return null;
        } finally {
            closeResultSetAndStatement(rs, st);
        }
    }

    private YeeExam queryExamById(Connection conn, Integer examId) throws Exception {
        String sql = "SELECT * FROM yee_exam WHERE id = ?";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, examId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    YeeExam exam = new YeeExam();
                    exam.setId(rs.getInt("id"));
                    exam.setUserId(rs.getInt("userId"));
                    exam.setTitle(rs.getString("title"));
                    exam.setTopicNumber(rs.getInt("topicNumber"));
                    exam.setScore(rs.getInt("score"));
                    exam.setStartTime(rs.getInt("startTime"));
                    exam.setEndTime(rs.getInt("endTime"));
                    exam.setRandom(rs.getInt("random"));
                    exam.setRandNumber(rs.getInt("randNumber"));
                    String randDataStr = rs.getString("randData");
                    if (randDataStr != null && !randDataStr.isEmpty()) {
                        exam.setRandData(JSON.parseObject(randDataStr, new TypeReference<Map<String, Integer>>() {}));
                    }
                    exam.setLimitedTime(rs.getInt("limitedTime"));
                    exam.setCourseId(rs.getInt("courseId"));
                    exam.setSchoolId(rs.getInt("schoolId"));
                    return exam;
                }
            }
        }
        return null;
    }

    private YeeCourse queryCourseById(Connection conn, Integer courseId) throws Exception {
        String sql = "SELECT * FROM yee_course WHERE id = ?";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, courseId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    YeeCourse course = new YeeCourse();
                    course.setId(rs.getLong("id"));
                    course.setName(rs.getString("name"));
                    course.setStartDate(rs.getDate("startDate"));
                    course.setEndDate(rs.getDate("endDate"));
                    return course;
                }
            }
        }
        return null;
    }

    /**
     * 根据recordId查询本场所有题目，返回是否存在主观简答题(type=4)
     */
    private boolean checkHasSubjectiveTopic(Connection conn, Integer recordId) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM yee_exam_answer a JOIN yee_exam_topic t ON a.topicId = t.id WHERE a.recordId = ? AND t.type = 4) AS hasSub";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, recordId);
        ResultSet rs = pst.executeQuery();
        boolean hasSub = false;
        if(rs.next()){
            hasSub = rs.getInt("hasSub") == 1;
        }
        closeResultSetAndStatement(rs, pst);
        return hasSub;
    }
    /**
     * 统一执行交卷后全部数据库更新（和学生手动交卷逻辑完全对齐）
     * @param conn 数据库连接
     * @param schoolId 学校ID
     * @param recordId 考试记录ID
     * @param examId 考试ID
     * @param userId 学生ID
     * @param courseId 课程ID
     * @param submitType 交卷类型 2教师收卷 /3自动超时收卷
     * @throws SQLException
     */
    private void doFinishExamTransaction(Connection conn, Integer schoolId, Integer recordId, Integer examId, Integer userId, Integer courseId, int submitType) throws Exception {
        // 1. 查询本场所有题目，计算客观题得分（复用原有算分逻辑）
        List<Map<String, Object>> topicList = queryStudentExamTopicsByRecordId(schoolId, recordId);
        List<YeeExamAnswer> answerList = queryYeeWorkAnswers(schoolId, recordId, examId, userId, courseId);
        List<Map<String, Object>> answerMap = answerList.stream()
                .map(yeeWorkAnswer -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", String.valueOf(yeeWorkAnswer.getId()));
                    map.put("recordId", String.valueOf(yeeWorkAnswer.getRecordId()));
                    map.put("examId", String.valueOf(yeeWorkAnswer.getExamId()));
                    map.put("userId", String.valueOf(yeeWorkAnswer.getUserId()));
                    map.put("courseId", String.valueOf(yeeWorkAnswer.getCourseId()));
                    map.put("topicId", String.valueOf(yeeWorkAnswer.getTopicId()));
                    map.put("answer", yeeWorkAnswer.getAnswer());
                    return map;
                }).collect(Collectors.toList());
        List<Map<String, Object>> calculateAnswerScores = ScoreCalculator.calculateAnswerScores(topicList, answerMap);

        // 2. 判断是否有主观题，确定最终state
        boolean hasSubjective = checkHasSubjectiveTopic(conn, recordId);
        int finalState = hasSubjective ? 2 : 3;
        long nowSec = System.currentTimeMillis() / 1000;

        // 3. 更新作答得分
        updateAnswerScores(conn, schoolId, calculateAnswerScores);

        // 4. 更新yee_exam_record 核心状态（区分submitType）
        String updateRecordSql = """
            UPDATE yee_exam_record
            SET state = ?, submitType = ?, submitTime = ?
            WHERE id = ? AND schoolId = ? AND state = 1
            """;
        PreparedStatement pstRecord = conn.prepareStatement(updateRecordSql);
        pstRecord.setInt(1, finalState);
        pstRecord.setInt(2, submitType);
        pstRecord.setInt(3, (int) nowSec);
        pstRecord.setInt(4, recordId);
        pstRecord.setInt(5, schoolId);
        int affectRow = pstRecord.executeUpdate();
        closeStatement(pstRecord);
        if(affectRow <= 0){
            // 无更新行数代表已交卷，抛出中断跳过该学生
            throw new SQLException("学生已完成交卷，无需重复操作");
        }

        // 5. 更新课程答题统计
        updateExamCountForCourse(conn, schoolId, courseId, userId);

        // 6. 更新yee_exam_score成绩表
        updateYeeWorkScore(conn, examId, userId, courseId, schoolId, calculateAnswerScores, finalState);
    }

    /**
     * 重载：支持传入事务连接，同一事务内插入record
     */
    private int insertYeeExamRecord(YeeExamRecord record, Connection conn) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        int generatedId = -1;
        try {
            String sql = """
        INSERT INTO yee_exam_record 
            (examId, userId, startTime, state, finishTime, score, 
             isCancel, frequency, teacherId, markTime, obScore, subScore, 
             markOrder, platform, courseId, classId, schoolId,
             submitType, submitTime, lastActiveTime, selectTopicIds)
        VALUES 
            (?, ?, ?, ?, ?, ?, 
             ?, ?, ?, ?, ?, ?, 
             ?, ?, ?, ?, ?,
             ?, ?, ?, ?)
        """;
            st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            st.setInt(1,  record.getExamId());
            st.setInt(2,  record.getUserid());
            st.setInt(3,  record.getStartTime());
            st.setInt(4,  record.getState());
            st.setInt(5,  record.getFinishTime());
            st.setBigDecimal(6, record.getScore());
            st.setInt(7,  record.getIsCancel());
            st.setInt(8,  record.getFrequency());
            st.setInt(9,  record.getTeacherId());
            st.setInt(10, record.getMarkTime());
            st.setBigDecimal(11, record.getObScore());
            st.setBigDecimal(12, record.getSubScore());
            st.setInt(13, record.getMarkOrder());
            st.setString(14, record.getPlatform());
            st.setInt(15, record.getCourseId());
            st.setInt(16, record.getClassId());
            st.setInt(17, record.getSchoolId());
            st.setInt(18, record.getSubmitType());
            st.setInt(19, record.getSubmitTime());
            st.setInt(20, record.getLastActiveTime());
            st.setString(21, record.getSelectTopicIds());

            int rowsAffected = st.executeUpdate();
            if (rowsAffected == 0) {
                throw new Exception("插入 yee_exam_record 影响行数为0");
            }
            rs = st.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            } else {
                throw new Exception("无法获取自增主键");
            }
            return generatedId;
        } finally {
            closeResultSetAndStatement(rs, st);
        }
    }

    /**
     * 重载：事务连接批量插入answer
     */
    private boolean insertYeeWorkAnswers(List<YeeExamAnswer> yeeWorkAnswers, Connection conn) throws Exception {
        if (yeeWorkAnswers == null || yeeWorkAnswers.isEmpty()) {
            return true;
        }
        PreparedStatement st = null;
        try {
            String sql = """
            INSERT INTO yee_exam_answer 
                (recordId, examId, topicId, answered, score, 
                 answer, images, files, marked, hit, userId, 
                 courseId, schoolId)
            VALUES 
                (?, ?, ?, ?, ?, ?, 
                 ?, ?, ?, ?, ?, ?, 
                 ?)
            """;
            st = conn.prepareStatement(sql);
            for (YeeExamAnswer answer : yeeWorkAnswers) {
                st.setInt(1,  answer.getRecordId());
                st.setInt(2,  answer.getExamId());
                st.setInt(3,  answer.getTopicId());
                st.setInt(4,  answer.getAnswered());
                st.setBigDecimal(5, answer.getScore());
                st.setString(6, answer.getAnswer());
                st.setString(7, answer.getImages());
                st.setString(8, answer.getFiles());
                st.setString(9, answer.getMarked());
                st.setInt(10, answer.getHit());
                st.setInt(11, answer.getUserId());
                st.setInt(12, answer.getCourseId());
                st.setInt(13, answer.getSchoolId());
                st.addBatch();
            }
            int[] results = st.executeBatch();
            for (int rows : results) {
                if (rows == Statement.EXECUTE_FAILED) {
                    return false;
                }
            }
            return true;
        } finally {
            closeStatement(st);
        }
    }

    /**
     * 重载：事务连接插入score
     */
    private void insertYeeWorkScore(Integer workId, Integer userId, Integer courseId, Integer schoolId, String platform, Connection conn) throws Exception {
        PreparedStatement st = null;
        try {
            String checkSql = "SELECT COUNT(*) FROM yee_exam_score WHERE examId = ? AND userId = ?";
            st = conn.prepareStatement(checkSql);
            st.setInt(1, workId);
            st.setInt(2, userId);
            ResultSet rs = st.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            closeResultSetAndStatement(rs, st);
            if (count > 0) {
                return;
            }
            String insertSql = """
                INSERT INTO yee_exam_score 
                (examId, userId, finalScore, state, scored, submitTime, timeCost, platform, courseId, schoolId)
                VALUES 
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            st = conn.prepareStatement(insertSql);
            st.setInt(1, workId);
            st.setInt(2, userId);
            st.setBigDecimal(3, new BigDecimal("0.00"));
            st.setInt(4, 1);
            st.setBoolean(5, false);
            st.setInt(6, 0);
            st.setInt(7, 0);
            st.setString(8, platform);
            st.setInt(9, courseId);
            st.setInt(10, schoolId);
            st.executeUpdate();
        } finally {
            closeStatement(st);
        }
    }
}