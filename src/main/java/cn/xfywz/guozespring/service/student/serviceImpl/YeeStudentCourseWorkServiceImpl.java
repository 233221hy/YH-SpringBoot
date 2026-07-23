package cn.xfywz.guozespring.service.student.serviceImpl;


import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.*;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.YeeStudentCourseWorkService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.ScoreCalculator;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.config.RabbitMQConfig;
import cn.xfywz.guozespring.entity.dto.WorkScoreMessage;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.jdbc.support.JdbcUtils.closeStatement;

@Slf4j
@Service
public class YeeStudentCourseWorkServiceImpl implements YeeStudentCourseWorkService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 学生课程作业列表
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param nodeId
     * @return Result
     * @throws Exception
     */
    @Transactional
    @Override
    public Result selectStudentWorkList(int schoolId, Integer courseId, Integer studentId, Integer nodeId) throws Exception {
        // 获取作业信息
        List<Map<String, Object>> workInfo = getWorkInfoByCourseAndStudent(schoolId, courseId, studentId, nodeId);

        // 循环封装次数、进入权限、状态提示
        for (Map<String, Object> work : workInfo) {
            Integer workId = (Integer) work.get("workId");
            // 1. 获取作业答题次数
            Integer frequency = getWorkFrequencyByUserAndWork(schoolId, studentId, workId);
            work.put("frequency", frequency);

            // 2. 查询学生该作业作答记录
            List<YeeWorkRecord> recordList = getExistingExamRecords(schoolId, workId, studentId);
            boolean canEnter;
            String enterTip;
            Integer workStatus; // 0无记录/1进行中可进入/2已提交不可进入

            if (recordList == null || recordList.isEmpty()) {
                // 无作答记录：首次可进入
                canEnter = true;
                enterTip = "可初次进入作业";
                workStatus = 0;
            } else {
                YeeWorkRecord record = recordList.get(0);
                if (Objects.equals(record.getState(), 1)) {
                    // state=3 进行中，允许重复进入
                    canEnter = true;
                    enterTip = "可继续上次作答";
                    workStatus = 1;
                } else {
                    // 已提交/批阅，禁止进入
                    canEnter = false;
                    enterTip = "作业已提交，不可重复进入";
                    workStatus = 2;
                }
            }
            // 前端展示字段
            work.put("canEnter", canEnter);
            work.put("enterTip", enterTip);
            work.put("workStatus", workStatus);
        }

        Map resultMap = new HashMap<>();
        resultMap.put("workInfo", workInfo);

        return Result.success(resultMap);
    }

    /**
     * 学生课程作业详情
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param workId
     * @return Result
     * @throws Exception
     */
    @Transactional
    @Override
    public Result selectStudentWorkDetail(int schoolId, Integer courseId, Integer studentId, Integer workId, String title) throws Exception {
        // 第一部分 信息
        List<Map<String, Object>> workDetail = getStudentWorkInfoByCourseAndStudent(schoolId, courseId, studentId);

        // 第二部分 信息过滤 只保留 title对应的 信息 stream流
        workDetail = workDetail.stream()
                .filter(work -> work.get("title").equals(title))
                .collect(Collectors.toList());

        Map resultMap = new HashMap<>();
        resultMap.put("workDetail", workDetail);

        return Result.success(resultMap);
    }

    /**
     * 作业开始答题
     * @param schoolId
     * @param courseId
     * @param userId
     * @param workId
     * @return
     */
    @Transactional
    @Override
    public Result startWork(int schoolId, Integer courseId, Integer userId, Integer workId,
                            Integer createUserId, String platform, Integer classId, Integer paperId) throws Exception {

        List<YeeWorkRecord> existingRecords = getExistingExamRecords(schoolId, workId, userId);
        // ============ 重复进入分支 ============
        // ============ 重复进入分支 ============
        if (existingRecords != null && !existingRecords.isEmpty()) {
            YeeWorkRecord activeRecord = existingRecords.get(0);
            Integer currRecordId = activeRecord.getId();
            // state=1 进行中可重复进入；其他状态（已提交/批阅）拦截
            if (!Objects.equals(activeRecord.getState(), 1)) {
                return Result.error("作业已提交/批阅，不允许重复进入");
            }
            long nowSec = System.currentTimeMillis() / 1000;
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            // 每次进入刷新最后活跃时间（用于定时超时收卷）
            updateWorkRecordLastActiveJdbc(slSchool, currRecordId, (int) nowSec);

            // 1、按当初顺序读取所有题目ID
            List<Integer> topicIdList = queryWorkTopicIdByRecordIdJdbc(slSchool, currRecordId);
            if (topicIdList.isEmpty()) {
                return Result.error("本场作业题目数据异常，请退出后重新创建记录");
            }
            // 2、查询完整题目信息
            List<Map<String, Object>> rawTopicList = queryTopicByIdListJdbc(slSchool, topicIdList);
            // 3、调用排序，还原原始抽题顺序
            rawTopicList = sortTopicByTopicIdList(topicIdList, rawTopicList);
            if(rawTopicList.size() != topicIdList.size()){
                return Result.error("本场作业存在缺失题目数据，请重新创建作业记录");
            }
            // 4、查询本场所有历史作答
            List<Map<String, Object>> answerList = queryAllWorkAnswerByRecordIdJdbc(slSchool, currRecordId);
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

                // 基础固定字段，和首次打开结构对齐
                item.put("recordId", currRecordId);
                item.put("number", numberSeq++);
                item.put("score", rawTopic.get("score"));
                item.put("workId", rawTopic.get("workId"));
                item.put("topic", rawTopic.get("topic"));
                item.put("id", tid);
                item.put("type", rawTopic.get("type"));

                // 选项JSON转数组，消除泛型警告
                String optionJson = (String) rawTopic.get("option");
                List<Map<String, Object>> optionArr = new ArrayList<>();
                if (optionJson != null && !optionJson.isBlank()) {
                    try {
                        List<?> tempOpt = JSON.parseArray(optionJson, Map.class);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> castOpt = (List<Map<String, Object>>) tempOpt;
                        optionArr = castOpt;
                    } catch (Exception e) {
                        // JSON破损/非法时返回空选项，不阻断整个题目加载
                        optionArr = new ArrayList<>();
                    }
                }
                item.put("option", optionArr);

                // 拼接历史作答数据
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
            resultMap.put("workTopics", formatTopicList);
            // 新增：外层返回recordId，和考试接口结构统一
            resultMap.put("recordId", currRecordId);
            return Result.success(resultMap);
        }
        // ==========================================

        // 原有首次创建作业逻辑 完全未改动
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        YeeWork work;
        YeeCourse course;
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            work = queryWorkById(conn, workId);
            if (work == null) {
                return Result.error("作业不存在");
            }
            course = queryCourseById(conn, courseId);
        }
        long nowSec = System.currentTimeMillis() / 1000;
        if (work.getStartTime() != null && work.getStartTime() > 0 && nowSec < work.getStartTime()) {
            return Result.error("作业尚未开始");
        }
        if (work.getEndTime() != null && work.getEndTime() > 0 && nowSec > work.getEndTime()) {
            return Result.error("作业已结束");
        }
        if (course != null && course.getStartDate() != null && course.getStartDate().after(new java.util.Date())) {
            return Result.error("课程尚未开课");
        }

        Map resultMap = new HashMap<>();

        YeeWorkRecord yeeWorkRecord = new YeeWorkRecord();
        yeeWorkRecord.setWorkId(workId);
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
        yeeWorkRecord.setEvalState(0);
        yeeWorkRecord.setMarkId(0);
        yeeWorkRecord.setClassId(classId);
        yeeWorkRecord.setSchoolId(schoolId);
        // 新字段初始化
        yeeWorkRecord.setSubmitType(0);
        yeeWorkRecord.setSubmitTime(0);
        yeeWorkRecord.setLastActiveTime((int) nowSec);

        int recordId = insertYeeWorkRecord(yeeWorkRecord);
        if (recordId == -1) {
            return Result.error("开始作业失败" + "yee_work_record 插入失败");
        }

        List<Map<String, Object>> maps = queryPaperTopicsAsMap(schoolId, workId, userId, recordId);
        if (maps.isEmpty()) {
            return Result.error("没有此试卷");
        }
        // 首次进入给每条题目塞recordId
        for (Map<String, Object> map : maps) {
            map.put("recordId", recordId);
        }
        resultMap.put("workTopics", maps);

        List<YeeWorkAnswer> yeeWorkAnswers = maps.stream()
                .map(map -> {
                    YeeWorkAnswer yeeWorkAnswer = new YeeWorkAnswer();
                    yeeWorkAnswer.setOid(0);
                    yeeWorkAnswer.setRecordId(recordId);
                    yeeWorkAnswer.setWorkId(workId);
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
                    yeeWorkAnswer.setIsEval(0);
                    yeeWorkAnswer.setMistakeDelete(0);
                    yeeWorkAnswer.setSchoolId(schoolId);
                    return yeeWorkAnswer;
                }).collect(Collectors.toList());

        boolean result = insertYeeWorkAnswers(yeeWorkAnswers);
        if (!result) {
            return Result.error("开始作业失败" + "yee_work_answer 插入失败");
        }

        insertYeeWorkScore(workId, userId, courseId, schoolId, platform);

        return Result.success(resultMap);
    }


// ===================== 作业专用新增工具方法 =====================

    /**
     * 将IN查询返回的无序题目列表，按answer表原始topicId顺序重排
     * @param sourceTopicIdList 从yee_*_answer取出的有序题目ID（原始抽题顺序）
     * @param unSortTopicList IN查询出来的乱序题目集合
     * @return 和ID顺序一一对应的有序题目列表
     */
    private List<Map<String, Object>> sortTopicByTopicIdList(List<Integer> sourceTopicIdList, List<Map<String, Object>> unSortTopicList) {
        // 构建 id -> 题目实体映射
        Map<Integer, Map<String, Object>> topicIdMap = new HashMap<>(unSortTopicList.size());
        for (Map<String, Object> topicRow : unSortTopicList) {
            Integer tid = (Integer) topicRow.get("id");
            topicIdMap.put(tid, topicRow);
        }
        // 按原始ID顺序遍历，还原展示顺序
        List<Map<String, Object>> sortedTopicList = new ArrayList<>(sourceTopicIdList.size());
        for (Integer tid : sourceTopicIdList) {
            Map<String, Object> targetTopic = topicIdMap.get(tid);
            if (targetTopic != null) {
                sortedTopicList.add(targetTopic);
            }
        }
        return sortedTopicList;
    }
    /**
     * 更新作业记录最后活跃时间（用于定时超时收卷）
     */
    private void updateWorkRecordLastActiveJdbc(SlSchool slSchool, Integer recordId, int nowSec) throws Exception {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "UPDATE yee_work_record SET lastActiveTime = ? WHERE id = ?";
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

    /**
     * 根据recordId查询作业所有题目ID，保留原始作答插入顺序
     */
    private List<Integer> queryWorkTopicIdByRecordIdJdbc(SlSchool slSchool, Integer recordId) throws Exception {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Integer> idList = new ArrayList<>();
        String sql = "SELECT id AS answerRowId,topicId FROM yee_work_answer WHERE recordId = ? ORDER BY id ASC";
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
     * 查询本场作业全部作答记录
     */
    private List<Map<String, Object>> queryAllWorkAnswerByRecordIdJdbc(SlSchool slSchool, Integer recordId) throws Exception {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT topicId, answer, images, files, hit, marked FROM yee_work_answer WHERE recordId = ?";
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
    }
    /**
     * 根据题目ID列表批量查询题目详情
     */
    private List<Map<String, Object>> queryTopicByIdListJdbc(SlSchool slSchool, List<Integer> idList) throws Exception {
        if (idList.isEmpty()) return new ArrayList<>();
        String[] placeholderArr = new String[idList.size()];
        Arrays.fill(placeholderArr, "?");
        String placeholders = String.join(",", placeholderArr);
        String sql = "SELECT id, topic, type, score, `option`, workId FROM yee_work_topic WHERE id IN (" + placeholders + ")";

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
     * 添加作业答案
     * @param schoolId
     * @param courseId
     * @param userId
     * @param answer
     * @param topicId
     * @param workId
     * @param recordId
     * @return
     * @throws Exception
     */
    @Override
    public Result addWorkAnswer(int schoolId, Integer courseId, Integer userId, List<String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception {
        boolean result = updateYeeWorkAnswer(schoolId, courseId, userId, answer, topicId, workId, recordId, type);
        if (result) {
            // 双重非空判断
            if (schoolId > 0 && courseId != null && userId != null && workId != null && recordId != null && topicId != null) {
                calculateSingleTopicScore(schoolId, courseId, userId, workId, recordId, topicId);
            }
        }
        return result ? Result.success("answer添加成功") : Result.error("answer添加失败");
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
     * @throws Exception
     */
    @Override
    public Result addWorkAnswerText(int schoolId, Integer courseId, Integer userId, String answer, Integer topicId, Integer workId, Integer recordId, Integer type, List<FileInfo> images, List<FileInfo> files) throws Exception {
        boolean result = updateYeeWorkAnswerText(schoolId, courseId, userId, answer, topicId, workId, recordId, type, images, files);
        if (result) {
            // 双重非空判断
            if (schoolId > 0 && courseId != null && userId != null && workId != null && recordId != null && topicId != null) {
                calculateSingleTopicScore(schoolId, courseId, userId, workId, recordId, topicId);
            }
        }
        return result ? Result.success("answer添加成功") : Result.error("answer添加失败");

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
     * @throws Exception
     */
    @Override
    public Result addWorkAnswerBlank(int schoolId, Integer courseId, Integer userId, Map<String, String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception {
        boolean result = updateYeeWorkAnswerBlank(schoolId, courseId, userId, answer, topicId, workId, recordId, type);
        if (result) {
            // 双重非空判断
            if (schoolId > 0 && courseId != null && userId != null && workId != null && recordId != null && topicId != null) {
                calculateSingleTopicScore(schoolId, courseId, userId, workId, recordId, topicId);
            }
        }
        return result ? Result.success("answer添加成功") : Result.error("answer添加失败");
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
    @Transactional(rollbackFor = Exception.class)
    public Result finishWorkAnswer(int schoolId, Integer courseId, Integer userId, Integer workId, Integer recordId) throws Exception {
        // 下面代码完全不用动，只加上面一行注解
        List<Map<String, Object>> maps = queryPaperTopicsAsMap(schoolId, workId, userId, recordId);
        if (maps == null || maps.isEmpty()) {
            return Result.error("未获取到作业题目");
        }

        List<YeeWorkAnswer> yeeWorkAnswers = queryYeeWorkAnswers(schoolId, recordId, workId, userId, courseId);

        List<Map<String, Object>> yeeWorkAnswersMap = yeeWorkAnswers.stream()
                .map(answer -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", answer.getId());
                    map.put("recordId", answer.getRecordId());
                    map.put("workId", answer.getWorkId());
                    map.put("userId", answer.getUserId());
                    map.put("courseId", answer.getCourseId());
                    map.put("topicId", answer.getTopicId());
                    map.put("answer", answer.getAnswer());
                    return map;
                }).collect(Collectors.toList());

        List<Map<String, Object>> calculateAnswerScores = ScoreCalculator.calculateAnswerScores(maps, yeeWorkAnswersMap);

        boolean hasSubjective = maps.stream()
                .map(m -> m.get("type"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .anyMatch("4"::equals);

        int state = hasSubjective ? 2 : 3;

        updateAnswerScores(schoolId, calculateAnswerScores);
        updateWorkRecordFinishState(schoolId, recordId, workId, userId, courseId, calculateAnswerScores, state);
        updateWorkCountForCourse(schoolId, courseId, userId);
        updateYeeWorkScore(workId, userId, courseId, schoolId, calculateAnswerScores, state);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("calculateAnswerScores", calculateAnswerScores);
        resultMap.put("yeeWorkTopic", maps);

        return Result.success(resultMap);
    }

    @Override
    public Result reStartWork(int schoolId, Integer courseId, Integer studentId, Integer workId, Integer createUserId, String platform, Integer classId, Integer paperId, Integer recordId) throws Exception {
        // 1. 删除 yee_work_record 根据recordId, 删除 yee_work_answer 根据recordId

        boolean isTrue = deleteYeeWorkDataByRecordId(recordId, schoolId);
        if (!isTrue) {
            return Result.error("重新开始作业失败" + "yee_work_record 删除失败");
        }

        Result result = startWork(schoolId, courseId, studentId, workId, createUserId, platform, classId, paperId);

        return result;
    }

    /**
     * 添加收藏
     * @param schoolId
     * @param userId
     * @param workId
     * @param topicId
     * @param courseId
     * @return
     * @throws Exception
     */
    @Override
    public Result addCollectionTopic(int schoolId, Integer userId, Integer workId, Integer topicId, Integer courseId) throws Exception {
        boolean result = saveCollectionTopic(schoolId, userId, topicId, workId, courseId);
        return result ? Result.success("添加成功") : Result.error("添加失败");
    }

    @Override
    public Result deleteCollectionTopic(int schoolId, Integer userId, Integer workId, Integer topicId, Integer courseId) throws Exception {
        boolean result = deleteCollectionTopics(schoolId, userId, topicId, workId, courseId);
        return result ? Result.success("删除成功") : Result.error("删除失败");
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
            String checkSql = "SELECT COUNT(*) FROM yee_work_score WHERE workId = ? AND userId = ?";
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
                INSERT INTO yee_work_score 
                (workId, userId, finalScore, state, scored, submitTime, timeCost, platform, courseId, schoolId)
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
            throw new Exception("插入 yee_work_score 失败，workId=" + workId + ", userId=" + userId + ", courseId=" + courseId + ", schoolId=" + schoolId, e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

    private boolean deleteYeeWorkDataByRecordId(Integer recordId, Integer schoolId) throws Exception {
        if (recordId == null || recordId <= 0) {
            return true; // 无效 ID 视为无需删除，返回成功
        }

        Connection conn = null;
        PreparedStatement st = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接（主库）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 4. 先删除 yee_work_answer（子表）
            String deleteAnswerSql = "DELETE FROM yee_work_answer WHERE recordId = ?";
            st = conn.prepareStatement(deleteAnswerSql);
            st.setInt(1, recordId);
            int answerRows = st.executeUpdate();
            closeStatement(st);

            // 5. 再删除 yee_work_record（主表）
            String deleteRecordSql = "DELETE FROM yee_work_record WHERE id = ?";
            st = conn.prepareStatement(deleteRecordSql);
            st.setInt(1, recordId);
            int recordRows = st.executeUpdate();

            // 6. 判断是否删除成功
            return answerRows >= 0 && recordRows >= 0; // DELETE 成功时返回 >=0

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("删除 yee_work_answer 和 yee_work_record 失败，recordId=" + recordId, e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

//    private void updateWorkRecordFinishState(int schoolId, Integer recordId, Integer workId, Integer userId, Integer courseId, List<Map<String, Object>> scoredResults, int state) throws Exception {
//        Connection conn = null;
//        PreparedStatement st = null;
//
//        // 计算总分
////        BigDecimal totalEarned = scoredResults.stream()
////                .map(item -> (BigDecimal) item.get("earnedScore"))
////                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal totalEarned = scoredResults.stream()
//                .map(item -> Optional.ofNullable((BigDecimal) item.get("earnedScore")).orElse(BigDecimal.ZERO))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        try {
//            // 1. 验证学校
//            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//            if (slSchool == null || slSchool.getAllow() == 0) {
//                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
//            }
//
//            // 2. 获取主库连接（写操作）
//            conn = SlaveMysqlConnectionUtil.getConnection(slSchool); // 注意：更新操作应使用主库
//
//            // 3. 构建 SQL
//            String sql = """
//                UPDATE yee_work_record
//                SET state = ?,
//                    finishTime = ?,
//                    score = ?,
//                    frequency = frequency + 1,
//                    markTime = ?,
//                    obScore = ?
//                WHERE id = ?
//                  AND workId = ?
//                  AND userId = ?
//                  AND courseId = ?
//                  AND schoolId = ?
//                """;
//
//            st = conn.prepareStatement(sql);
//
//            long timeMillis = System.currentTimeMillis() / 1000;
//
//            // 4. 设置参数
//            int paramIndex = 1;
//            st.setInt(paramIndex++, state); // 动态设置状态值
//            st.setObject(paramIndex++, timeMillis);
//            st.setObject(paramIndex++, totalEarned); // 使用 setObject 允许 null
//            st.setObject(paramIndex++, timeMillis);
//            st.setObject(paramIndex++, totalEarned);
//            st.setInt(paramIndex++, recordId);
//            st.setInt(paramIndex++, workId);
//            st.setInt(paramIndex++, userId);
//            st.setInt(paramIndex++, courseId);
//            st.setInt(paramIndex, schoolId);
//
//            // 5. 执行更新
//            int rowsAffected = st.executeUpdate();
//
//            // 可选：判断是否更新成功
//            if (rowsAffected == 0) {
//                throw new Exception("更新作业记录失败，未找到匹配记录。" +
//                        "recordId=" + recordId + ", workId=" + workId +
//                        ", userId=" + userId + ", schoolId=" + schoolId);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new Exception("更新 yee_work_record 状态失败，recordId=" + recordId + ", workId=" + workId, e);
//        } finally {
//            // 安全关闭资源
//            closeResultSetAndStatement(null, st);
//            closeConnection(conn);
//        }
//    }
private void updateWorkRecordFinishState(int schoolId, Integer recordId, Integer workId, Integer userId, Integer courseId, List<Map<String, Object>> scoredResults, int state) throws Exception {
    Connection conn = null;
    PreparedStatement st = null;

    // 计算总分
    BigDecimal totalEarned = scoredResults.stream()
            .map(item -> Optional.ofNullable((BigDecimal) item.get("earnedScore")).orElse(BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    try {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
        }

        // 2. 获取主库连接（写操作）
        conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

        // 3. 构建 SQL，新增 submit_type、submit_time
        String sql = """
            UPDATE yee_work_record 
            SET state = ?, 
                finishTime = ?, 
                score = ?, 
                frequency = frequency + 1, 
                markTime = ?, 
                obScore = ?,
                submitType = ?,
                submitTime = ?
            WHERE id = ?
              AND workId = ?
              AND userId = ?
              AND courseId = ?
              AND schoolId = ?
            """;

        st = conn.prepareStatement(sql);

        long timeMillis = System.currentTimeMillis() / 1000;
        int submitType = 1; // 1=学生手动提交

        // 4. 设置参数，顺序同步SQL新增两列
        int paramIndex = 1;
        st.setInt(paramIndex++, state);                 // state
        st.setInt(paramIndex++, (int) timeMillis);      // finishTime
        st.setBigDecimal(paramIndex++, totalEarned);    // score
        st.setInt(paramIndex++, (int) timeMillis);      // markTime
        st.setBigDecimal(paramIndex++, totalEarned);    // obScore
        st.setInt(paramIndex++, submitType);            // submit_type 手动提交
        st.setInt(paramIndex++, (int) timeMillis);      // submit_time
        st.setInt(paramIndex++, recordId);
        st.setInt(paramIndex++, workId);
        st.setInt(paramIndex++, userId);
        st.setInt(paramIndex++, courseId);
        st.setInt(paramIndex, schoolId);

        // 5. 执行更新
        int rowsAffected = st.executeUpdate();

        // 可选：判断是否更新成功
        if (rowsAffected == 0) {
            throw new Exception("更新作业记录失败，未找到匹配记录。" +
                    "recordId=" + recordId + ", workId=" + workId +
                    ", userId=" + userId + ", schoolId=" + schoolId);
        }

    } catch (Exception e) {
        e.printStackTrace();
        throw new Exception("更新 yee_work_record 状态失败，recordId=" + recordId + ", workId=" + workId, e);
    } finally {
        // 安全关闭资源
        closeResultSetAndStatement(null, st);
        closeConnection(conn);
    }
}

    private void updateAnswerScores(int schoolId, List<Map<String, Object>> scoredResults) throws Exception {
        if (scoredResults == null || scoredResults.isEmpty()) {
            return;
        }

        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
        }

        String sql = """
        UPDATE yee_work_answer 
        SET score = ?, marked = 1, hit = ? 
        WHERE recordId = ? 
          AND workId = ? 
          AND topicId = ? 
          AND userId = ? 
          AND courseId = ? 
          AND schoolId = ?
        """;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setQueryTimeout(10); // 【新增】设置查询超时，防止慢查询夯死

            for (Map<String, Object> item : scoredResults) {
                BigDecimal earnedScore = getBigDecimal(item, "earnedScore");
                Integer correctStatus = getInteger(item, "correctStatus");
                Integer recordId = getInteger(item, "recordId");
                Integer workId = getInteger(item, "workId");
                Integer topicId = getInteger(item, "topicId");
                Integer userId = getInteger(item, "userId");
                Integer courseId = getInteger(item, "courseId");

                ps.setBigDecimal(1, earnedScore);
                ps.setInt(2, correctStatus);
                ps.setInt(3, recordId);
                ps.setInt(4, workId);
                ps.setInt(5, topicId);
                ps.setInt(6, userId);
                ps.setInt(7, courseId);
                ps.setInt(8, schoolId);
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            if (e.getMessage().contains("Deadlock found")) {
                Thread.sleep(100);
                // 【删除】无限递归
                // updateAnswerScores(schoolId, scoredResults);

                // 【替换】最多重试2次，防止栈溢出
                retryUpdateAnswerScores(schoolId, scoredResults, 0);
            } else {
                throw new RuntimeException("数据库更新失败", e);
            }
        }
    }

    // 【新增】重试方法，带次数限制
    private void retryUpdateAnswerScores(int schoolId, List<Map<String, Object>> scoredResults, int retryCount) throws Exception {
        final int MAX_RETRY = 2;
        if (retryCount > MAX_RETRY) {
            throw new Exception("死锁重试次数超限，更新答案失败");
        }
        try {
            updateAnswerScores(schoolId, scoredResults);
        } catch (SQLException e) {
            if (e.getMessage().contains("Deadlock found")) {
                Thread.sleep(100);
                retryUpdateAnswerScores(schoolId, scoredResults, retryCount + 1);
            } else {
                throw e;
            }
        }
    }

    /**
     * 根据条件查询 yee_work_answer 记录
     *
     * @param schoolId  学校ID
     * @param recordId  记录ID
     * @param workId    作业ID
     * @param userId    用户ID
     * @param courseId  课程ID
     * @return 查询结果列表
     * @throws Exception 查询失败
     */
    private List<YeeWorkAnswer> queryYeeWorkAnswers(Integer schoolId, Integer recordId,
                                                    Integer workId, Integer userId,
                                                    Integer courseId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<YeeWorkAnswer> resultList = new ArrayList<>();

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
            FROM yee_work_answer
            WHERE schoolId = ? 
              AND recordId = ?
              AND workId = ?
              AND userId = ?
              AND courseId = ?
            """;

            st = conn.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, recordId);
            st.setInt(3, workId);
            st.setInt(4, userId);
            st.setInt(5, courseId);

            // 4. 执行查询
            rs = st.executeQuery();

            // 5. 封装结果
            ObjectMapper objectMapper = OBJECT_MAPPER; // 用于 JSON 反序列化
            while (rs.next()) {
                YeeWorkAnswer answer = new YeeWorkAnswer();
                answer.setId(rs.getInt("id"));
                answer.setOid(rs.getInt("oid"));
                answer.setRecordId(rs.getInt("recordId"));
                answer.setWorkId(rs.getInt("workId"));
                answer.setTopicId(rs.getInt("topicId"));
                answer.setAnswered(rs.getInt("answered"));
                answer.setScore(rs.getBigDecimal("score"));
                answer.setAnswer(rs.getString("answer"));
                answer.setMarked(rs.getString("marked"));
                answer.setRemark(rs.getString("remark"));
                answer.setHit(rs.getInt("hit"));
                answer.setUserId(rs.getInt("userId"));
                answer.setCourseId(rs.getInt("courseId"));
                answer.setIsEval(rs.getInt("isEval"));
                answer.setMistakeDelete(rs.getInt("mistakeDelete"));
                answer.setSchoolId(rs.getInt("schoolId"));

                resultList.add(answer);
            }

            return resultList;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询 yee_work_answer 失败，条件: schoolId=" + schoolId +
                    ", recordId=" + recordId +
                    ", workId=" + workId +
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
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
        }

        String answerJson = "";

        if (answer == null || answer.isEmpty()) {
            answerJson = "";
        } else if(type == 2) {
            answerJson = new ObjectMapper().writeValueAsString(answer);
        } else {
            answerJson = answer.get(0);
        }

        String sql = """
            UPDATE yee_work_answer SET answered = 1, answer = ? 
            WHERE recordId = ? AND workId = ? AND topicId = ? AND userId = ? AND courseId = ? AND schoolId = ?
            """;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, answerJson);
            st.setInt(2, recordId);
            st.setInt(3, workId);
            st.setInt(4, topicId);
            st.setInt(5, userId);
            st.setInt(6, courseId);
            st.setInt(7, schoolId);

            return st.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("Deadlock found") || e.getMessage().contains("lock wait")) {
                Thread.sleep(100);
                return updateYeeWorkAnswer(schoolId, courseId, userId, answer, topicId, workId, recordId, type);
            }
            throw e;
        }
    }


    /**
     * 根据 YeeWorkAnswer 对象更新指定记录的 answered 和 answer 字段
     *
     * @param answer
     * @return 是否更新成功
     * @throws Exception 更新失败时抛出异常
     */
    private boolean updateYeeWorkAnswerText(Integer schoolId, Integer courseId, Integer userId, String answer, Integer topicId, Integer workId, Integer recordId, Integer type, List<FileInfo> images, List<FileInfo> files) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
        }

        ObjectMapper objectMapper = OBJECT_MAPPER;
        String imagesStr = objectMapper.writeValueAsString(images);
        String filesStr = objectMapper.writeValueAsString(files);

        String sql = """
            UPDATE yee_work_answer SET answered = 1, answer = ?, images = ?, files = ? 
            WHERE recordId = ? AND workId = ? AND topicId = ? AND userId = ? AND courseId = ? AND schoolId = ?
            """;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, answer);
            st.setString(2, imagesStr);
            st.setString(3, filesStr);
            st.setInt(4, recordId);
            st.setInt(5, workId);
            st.setInt(6, topicId);
            st.setInt(7, userId);
            st.setInt(8, courseId);
            st.setInt(9, schoolId);

            return st.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("Deadlock found") || e.getMessage().contains("lock wait")) {
                Thread.sleep(100);
                return updateYeeWorkAnswerText(schoolId, courseId, userId, answer, topicId, workId, recordId, type, images, files);
            }
            throw e;
        }
    }

    /**
     * 根据 YeeWorkAnswer 对象更新指定记录的 answered 和 answer 字段
     *
     * @param answer
     * @return 是否更新成功
     * @throws Exception 更新失败时抛出异常
     */
    private boolean updateYeeWorkAnswerBlank(Integer schoolId, Integer courseId, Integer userId, Map<String, String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
        }

        String answerStr = OBJECT_MAPPER.writeValueAsString(answer);

        String sql = """
            UPDATE yee_work_answer SET answered = 1, answer = ? 
            WHERE recordId = ? AND workId = ? AND topicId = ? AND userId = ? AND courseId = ? AND schoolId = ?
            """;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setString(1, answerStr);
            st.setInt(2, recordId);
            st.setInt(3, workId);
            st.setInt(4, topicId);
            st.setInt(5, userId);
            st.setInt(6, courseId);
            st.setInt(7, schoolId);

            return st.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("Deadlock found") || e.getMessage().contains("lock wait")) {
                Thread.sleep(100);
                return updateYeeWorkAnswerBlank(schoolId, courseId, userId, answer, topicId, workId, recordId, type);
            }
            throw e;
        }
    }


    /**
     * 批量插入作业答题记录
     *
     * @param yeeWorkAnswers 答题记录列表
     * @return 是否全部插入成功
     * @throws Exception 插入失败
     */
    private boolean insertYeeWorkAnswers(List<YeeWorkAnswer> yeeWorkAnswers) throws Exception {
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
            INSERT INTO yee_work_answer 
                (oid, recordId, workId, topicId, answered, score, 
                 answer, images, files, marked, hit, userId, 
                 courseId, isEval, mistakeDelete, schoolId)
            VALUES 
                (?, ?, ?, ?, ?, ?, 
                 ?, ?, ?, ?, ?, ?, 
                 ?, ?, ?, ?)
            """;

            st = conn.prepareStatement(sql);

            // 5. 遍历列表，设置参数并添加到批处理
            for (YeeWorkAnswer answer : yeeWorkAnswers) {
                st.setInt(1,  answer.getOid());
                st.setInt(2,  answer.getRecordId());
                st.setInt(3,  answer.getWorkId());
                st.setInt(4,  answer.getTopicId());
                st.setInt(5,  answer.getAnswered());
                st.setBigDecimal(6, answer.getScore());
                st.setString(7, answer.getAnswer());
                st.setString(8, answer.getImages());
                st.setString(9, answer.getFiles());
                st.setString(10, answer.getMarked());
                st.setInt(11, answer.getHit());
                st.setInt(12, answer.getUserId());
                st.setInt(13, answer.getCourseId());
                st.setInt(14, answer.getIsEval());
                st.setInt(15, answer.getMistakeDelete());
                st.setInt(16, answer.getSchoolId());

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
            String workId = yeeWorkAnswers.isEmpty() ? "unknown" : String.valueOf(yeeWorkAnswers.get(0).getWorkId());
            Integer userId = yeeWorkAnswers.isEmpty() ? -1 : yeeWorkAnswers.get(0).getUserId();
            throw new Exception("批量插入 yee_work_answer 失败，workId=" + workId +
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
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 2. 获取从库连接（读操作）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL
            String sql = """
            
                    SELECT wt.id,
                          wt.number,
                          wt.type,
                          wt.score,
                          wt.topic,
                          wt.option,
                          wt.workId,
                          wr.id AS recordId
                   FROM yee_work_topic wt
                   LEFT JOIN yee_work_record wr on wr.workId = wt.workId
                   WHERE wt.workId = ?
                   AND wr.userId = ?
                   AND wr.id = ?
            """;

            st = conn.prepareStatement(sql);
            st.setInt(1, workId);
            st.setInt(2, userId);
            st.setInt(3, recordId);

            // 4. 执行查询
            rs = st.executeQuery();

            // 5. 获取元数据（用于动态列）
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 7. 遍历结果集，动态封装每行数据
            ObjectMapper objectMapper = OBJECT_MAPPER;
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // 使用别名或列名
                    Object value = rs.getObject(i);

                    // 特殊处理：将 option 字段从 JSON 字符串转为 List<Map>
                    if ("option".equalsIgnoreCase(columnName) && value != null) {
                        try {
                            value = objectMapper.readValue(value.toString(), List.class);
                        } catch (Exception e) {
                            // 解析失败则保留原始字符串
                            e.printStackTrace();
                        }
                    }

                    // 新增：处理 answer 字段（如果是多选题）
                    if ("answer".equalsIgnoreCase(columnName) && value instanceof String) {
                        String answerStr = (String) value;
                        try {
                            // 尝试解析成 List<String>
                            if (answerStr.trim().startsWith("[")) {
                                value = objectMapper.readValue(answerStr, List.class);
                            }
                            // 如果不是数组格式，保持原样（如单选 "A"）
                        } catch (Exception e) {
                            // 解析失败，保留原始字符串
                        }
                    }

                    // 其他字段直接放入
                    row.put(columnName, value);

                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询 yee_paper_topic 失败，workId=" + workId + ", schoolId=" + schoolId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 插入一条作业记录，并返回自增主键 id
     *
     * @param record YeeWorkRecord 对象
     * @return 自增主键 id（> 0 表示成功）
     * @throws Exception 插入失败或获取 ID 失败
     */
    private int insertYeeWorkRecord(YeeWorkRecord record) throws Exception {
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

            // 3. SQL 插入语句（要求返回自增主键）
            String sql = """
            INSERT INTO yee_work_record 
                (workId, userId, startTime, state, finishTime, score, 
                 isCancel, frequency, teacherId, markTime, obScore, subScore, 
                 markOrder, platform, courseId, evalState, markId, classId, schoolId)
            VALUES 
                (?, ?, ?, ?, ?, ?, 
                 ?, ?, ?, ?, ?, ?, 
                 ?, ?, ?, ?, ?, ?, ?)
            """;

            // 传入 Statement.RETURN_GENERATED_KEYS，表示需要返回生成的主键
            st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // 4. 设置参数（严格对应 SQL 字段顺序）
            st.setInt(1,  record.getWorkId());
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
            st.setInt(16, record.getEvalState());
            st.setInt(17, record.getMarkId());
            st.setInt(18, record.getClassId());
            st.setInt(19, record.getSchoolId());

            // 5. 执行插入
            int rowsAffected = st.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("插入 yee_work_record 失败，影响行数为 0");
            }

            // 6. 获取自增主键
            rs = st.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1); // 获取第一个生成的主键
            } else {
                throw new Exception("未能获取自增主键");
            }

            // 7. 返回主键
            return generatedId;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("插入 yee_work_record 失败，workId=" + record.getWorkId() +
                    ", userId=" + record.getUserid() +
                    ", schoolId=" + record.getSchoolId(), e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private boolean deleteCollectionTopics(
            Integer schoolId,
            Integer userId,
            Integer topicId,
            Integer workId,
            Integer courseId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接（主库）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建删除 SQL（逻辑：根据用户、试题、课程删除）
            String sql = """
                DELETE FROM yee_collection_topic 
                WHERE 
                    userId = ? 
                    AND topicId = ?
                    AND courseId = ?
                    AND schoolId = ?
                    AND workId = ?
                """;

            st = conn.prepareStatement(sql);

            // 4. 设置参数
            st.setInt(1, userId);
            st.setInt(2, topicId);
            st.setInt(3, courseId);
            st.setInt(4, schoolId);
            st.setInt(5, workId);

            // 5. 执行删除
            int rowsAffected = st.executeUpdate();

            // 6. 返回是否删除成功
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("删除收藏记录失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", topicId=" + topicId +
                    ", workId=" + workId +
                    ", courseId=" + courseId, e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

    private boolean saveCollectionTopic(
            Integer schoolId,
            Integer userId,
            Integer topicId,
            Integer workId,
            Integer courseId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 先检查是否已收藏
            String checkSql = """
            SELECT id FROM yee_collection_topic 
            WHERE userId = ? AND topicId = ? AND courseId = ? AND schoolId = ? AND workId = ?
            LIMIT 1
            """;

            st = conn.prepareStatement(checkSql);
            st.setInt(1, userId);
            st.setInt(2, topicId);
            st.setInt(3, courseId);
            st.setInt(4, schoolId);
            st.setInt(5, workId);


            rs = st.executeQuery();
            if (rs.next()) {
                // 已存在，无需插入
                throw new Exception("已收藏");
            }

            // 关闭查询资源
            closeResultSetAndStatement(rs, st);

            // 插入新记录
            String insertSql = """
            INSERT INTO yee_collection_topic 
                (userId, topicId, workId, addTime, courseId, schoolId) 
            VALUES 
                (?, ?, ?, NOW(), ?, ?)
            """;

            st = conn.prepareStatement(insertSql);
            st.setInt(1, userId);
            st.setInt(2, topicId);
            st.setInt(3, workId != null ? workId : 0);
            st.setInt(4, courseId);
            st.setInt(5, schoolId);

            int rowsAffected = st.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("新增收藏记录失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", topicId=" + topicId +
                    ", workId=" + workId +
                    ", courseId=" + courseId, e);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getStudentWorkInfoByCourseAndStudent(
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
                    cs.classId
                FROM 
                    yee_course_student cs
                    LEFT JOIN yee_student s ON s.id = cs.studentId
                    LEFT JOIN yee_course_class c ON c.id = cs.classId
                    LEFT JOIN yee_work w ON w.courseId = cs.courseId AND ( JSON_LENGTH(w.classList) = 0 OR JSON_CONTAINS(w.classList, CAST(cs.classId AS JSON)))
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

    private Integer getWorkFrequencyByUserAndWork(
            Integer schoolId,
            Integer userId,
            Integer workId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        Integer frequency = null; // 默认返回 null

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
                    yee_work_record wr
                WHERE 
                    wr.userId = ?
                    AND wr.workId = ?
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
            throw new Exception("查询作业记录 frequency 失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", workId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getWorkInfoByCourseAndStudent(
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
                    w.id AS workId,
                    w.title,
                    w.startTime,
                    w.endTime,
                    w.score,
                    w.nodeId
                FROM 
                    yee_course_student cs
                    LEFT JOIN yee_work w ON w.courseId = cs.courseId AND ( JSON_LENGTH(w.classList) = 0 OR JSON_CONTAINS(w.classList, CAST(cs.classId AS JSON)))
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
     * 更新课程学生表中的作业数量
     * @param schoolId 学校ID
     * @param courseId 课程ID
     * @param userId 用户ID
     * @throws Exception
     */
    private void updateWorkCountForCourse(int schoolId, Integer courseId, Integer userId) throws Exception {
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

            // 3. 查询该学生在该课程中的作业完成数量（已完成的作业记录数）
            String countWorkSql = "SELECT COUNT(*) as workCount FROM yee_work_record WHERE courseId = ? AND userId = ? AND (state = 3 or state = 2)";
            st = conn.prepareStatement(countWorkSql);
            st.setInt(1, courseId);
            st.setInt(2, userId);
            rs = st.executeQuery();

            int workCount = 0;
            if (rs.next()) {
                workCount = rs.getInt("workCount");
            }

            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 4. 更新 yee_course_student 表中的 workLearned 字段
            String updateSql = "UPDATE yee_course_student SET workLearned = ? WHERE courseId = ? AND studentId = ?";
            st = conn.prepareStatement(updateSql);
            st.setInt(1, workCount);
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
                UPDATE yee_work_score 
                SET state = ?, 
                    scored = 1, 
                    submitTime = ?, 
                    finalScore = ?,
                    timeCost = (SELECT finishTime - startTime FROM yee_work_record WHERE workId = ? AND userId = ? LIMIT 1)
                WHERE workId = ? 
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
            throw new Exception("更新 yee_work_score 失败，workId=" + workId + ", userId=" + userId + ", courseId=" + courseId + ", schoolId=" + schoolId, e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

    /**
     * 查询已有考试记录
     * @param schoolId 学校ID
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 已有考试记录列表
     * @throws Exception 查询失败
     */
    private List<YeeWorkRecord> getExistingExamRecords(Integer schoolId, Integer examId, Integer userId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<YeeWorkRecord> resultList = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核，schoolId=" + schoolId);
            }

            // 2. 获取从库连接（读操作）
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. SQL 查询语句
            String sql = "SELECT * FROM yee_work_record WHERE schoolId = ? AND workId = ? AND userId = ?";

            st = conn.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, examId);
            st.setInt(3, userId);

            // 4. 执行查询
            rs = st.executeQuery();

            // 5. 封装结果
            while (rs.next()) {
                YeeWorkRecord record = new YeeWorkRecord();
                record.setId(rs.getInt("id"));
                record.setWorkId(rs.getInt("workId"));
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
            throw new Exception("查询 yee_work_record 失败，条件: schoolId=" + schoolId +
                    ", examId=" + examId +
                    ", userId=" + userId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private void calculateSingleTopicScore(
            int schoolId,
            Integer courseId,
            Integer userId,
            Integer workId,
            Integer recordId,
            Integer topicId) throws Exception {

        if (courseId == null || userId == null || workId == null || recordId == null || topicId == null) {
            return;
        }

        Connection conn = null;
        try {
            conn = databaseUtil.getConnection(schoolId);

            List<Map<String, Object>> singleTopic = querySingleWorkTopic(conn, workId, topicId);
            if (singleTopic == null || singleTopic.isEmpty()) {
                return;
            }

            YeeWorkAnswer singleAnswer = querySingleWorkAnswer(conn, schoolId, recordId, topicId);
            if (singleAnswer == null) {
                return;
            }

            // 如果该题已批改过，跳过 MQ 发送（防止重复累加分数）
            boolean alreadyMarked = "1".equals(singleAnswer.getMarked());

            List<Map<String, Object>> answerList = new ArrayList<>();
            Map<String, Object> ansMap = new HashMap<>();
            ansMap.put("topicId", singleAnswer.getTopicId());
            ansMap.put("answer", singleAnswer.getAnswer());
            ansMap.put("recordId", singleAnswer.getRecordId());
            ansMap.put("workId", singleAnswer.getWorkId());
            ansMap.put("userId", singleAnswer.getUserId());
            ansMap.put("courseId", singleAnswer.getCourseId());
            answerList.add(ansMap);

            List<Map<String, Object>> scoreList = ScoreCalculator.calculateAnswerScores(singleTopic, answerList);
            if (scoreList == null || scoreList.isEmpty()) {
                return;
            }

            BigDecimal earnedScore = ScoreCalculator.extractEarnedScore(scoreList);

            // 更新单题得分
            updateAnswerScores(conn, schoolId, scoreList);

            if (!alreadyMarked) {
                WorkScoreMessage msg = new WorkScoreMessage(
                        schoolId, recordId, workId, topicId, userId, courseId, earnedScore);
                rabbitTemplate.convertAndSend(RabbitMQConfig.WORK_EXCHANGE,
                        RabbitMQConfig.WORK_ROUTING_KEY, msg);
            }
        } finally {
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> querySingleWorkTopic(Connection conn, Integer workId, Integer topicId) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String sql = "SELECT wt.id, wt.number, wt.type, wt.score, wt.topic, wt.option FROM yee_work_topic wt WHERE wt.workId = ? AND wt.id = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, workId);
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

    private YeeWorkAnswer querySingleWorkAnswer(Connection conn, Integer schoolId, Integer recordId, Integer topicId) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM yee_work_answer WHERE schoolId = ? AND recordId = ? AND topicId = ? LIMIT 1";
            st = conn.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, recordId);
            st.setInt(3, topicId);
            rs = st.executeQuery();

            if (rs.next()) {
                YeeWorkAnswer answer = new YeeWorkAnswer();
                answer.setId(rs.getInt("id"));
                answer.setOid(rs.getInt("oid"));
                answer.setRecordId(rs.getInt("recordId"));
                answer.setWorkId(rs.getInt("workId"));
                answer.setTopicId(rs.getInt("topicId"));
                answer.setAnswered(rs.getInt("answered"));
                answer.setScore(rs.getBigDecimal("score"));
                answer.setAnswer(rs.getString("answer"));
                answer.setMarked(rs.getString("marked"));
                answer.setRemark(rs.getString("remark"));
                answer.setHit(rs.getInt("hit"));
                answer.setUserId(rs.getInt("userId"));
                answer.setCourseId(rs.getInt("courseId"));
                answer.setIsEval(rs.getInt("isEval"));
                answer.setMistakeDelete(rs.getInt("mistakeDelete"));
                answer.setSchoolId(rs.getInt("schoolId"));
                return answer;
            }
            return null;
        } finally {
            closeResultSetAndStatement(rs, st);
        }
    }

    private void updateAnswerScores(Connection conn, int schoolId, List<Map<String, Object>> scoredResults) throws Exception {
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE yee_work_answer SET score = ?, marked = 1, hit = ? " +
                    "WHERE recordId = ? AND workId = ? AND topicId = ? AND userId = ? AND courseId = ? AND schoolId = ?";
            ps = conn.prepareStatement(sql);
            for (Map<String, Object> item : scoredResults) {
                ps.setBigDecimal(1, getBigDecimal(item, "earnedScore"));
                ps.setInt(2, getInteger(item, "correctStatus"));
                ps.setInt(3, getInteger(item, "recordId"));
                ps.setInt(4, getInteger(item, "workId"));
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

    private YeeWork queryWorkById(Connection conn, Integer workId) throws Exception {
        String sql = "SELECT id, startTime, endTime FROM yee_work WHERE id = ?";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, workId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    YeeWork work = new YeeWork();
                    work.setId(rs.getInt("id"));
                    work.setStartTime(rs.getInt("startTime"));
                    work.setEndTime(rs.getInt("endTime"));
                    return work;
                }
            }
        }
        return null;
    }

    private YeeCourse queryCourseById(Connection conn, Integer courseId) throws Exception {
        String sql = "SELECT id, startDate, endDate FROM yee_course WHERE id = ?";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, courseId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    YeeCourse course = new YeeCourse();
                    course.setId(rs.getLong("id"));
                    course.setStartDate(rs.getDate("startDate"));
                    course.setEndDate(rs.getDate("endDate"));
                    return course;
                }
            }
        }
        return null;
    }

    @Transactional
//    @Override
    public Result teacherBatchCollectWork(int schoolId, Integer workId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        Connection mainConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        mainConn.setAutoCommit(false); // 批量统一事务

        int successCount = 0;
        List<Integer> failStudentIds = new ArrayList<>();
        Map<Integer, String> failMsgMap = new HashMap<>();
        List<Map<String, Integer>> allUnSubmitRecord = new ArrayList<>();

        try {
            // 查询本场作业所有进行中记录 state=1
            String queryUnSubmitSql = """
            SELECT id AS recordId, userId, courseId
            FROM yee_work_record
            WHERE schoolId = ? AND workId = ? AND state = 1
            """;
            PreparedStatement pstQuery = mainConn.prepareStatement(queryUnSubmitSql);
            pstQuery.setInt(1, schoolId);
            pstQuery.setInt(2, workId);
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
                return Result.success("本场作业暂无未提交学生，无需收卷");
            }

            // 循环强制收卷 submitType=2 教师强制
            for (Map<String, Integer> item : allUnSubmitRecord) {
                Integer recordId = item.get("recordId");
                Integer userId = item.get("userId");
                Integer courseId = item.get("courseId");
                try {
                    doFinishWorkTransaction(mainConn, schoolId, recordId, workId, userId, courseId, 2);
                    successCount++;
                } catch (SQLException e) {
                    failStudentIds.add(userId);
                    failMsgMap.put(userId, e.getMessage());
                }
            }
            mainConn.commit();
        } catch (Exception e) {
            mainConn.rollback();
            throw new Exception("作业批量收卷异常，全部操作已回滚：" + e.getMessage());
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


    // 定时10分钟，任务执行完成后延时10分钟再跑下一轮
    // 任务跑完再等10分钟才下一轮，杜绝重叠并发
    @Scheduled(fixedDelay = 600000)
    public void scheduleAutoTimeoutCollectWork() {
        log.info("【作业定时收卷】本轮全局扫描开始");
        try {
            List<Map<String, Integer>> allSchoolWorkList = getAllSchoolWorkList();
            if (allSchoolWorkList.isEmpty()) {
                return;
            }
            // 按schoolId分组，同一个学校的作业放一组，实现一校一校串行
            Map<Integer, List<Integer>> schoolWorkGroup = new HashMap<>();
            for (Map<String, Integer> swMap : allSchoolWorkList) {
                Integer schoolId = swMap.get("schoolId");
                Integer workId = swMap.get("workId");
                if (schoolId == null || workId == null) {
                    log.warn("【作业定时收卷】数据异常，schoolId={}, workId为空", schoolId);
                    continue;
                }
                schoolWorkGroup.computeIfAbsent(schoolId, k -> new ArrayList<>()).add(workId);
            }

            // 遍历每一所学校，处理完当前整校所有作业，sleep5分钟再处理下一所
            List<Integer> schoolIdList = new ArrayList<>(schoolWorkGroup.keySet());
            for (int i = 0; i < schoolIdList.size(); i++) {
                Integer schoolId = schoolIdList.get(i);
                List<Integer> workIdList = schoolWorkGroup.get(schoolId);
                log.info("【作业定时收卷】开始处理 schoolId={}, 待处理作业数:{}", schoolId, workIdList.size());
                SlSchool slSchool = slSchoolMapper.selectById(schoolId);
                for (Integer workId : workIdList) {
                    try {
                        autoTimeoutCollectWork(schoolId, workId, slSchool);
                    } catch (Exception e) {
                        log.error("【作业定时收卷】单作业扫描失败 schoolId={},workId={}", schoolId, workId, e);
                    }
                }
                log.info("【作业定时收卷】schoolId={} 全部作业处理完成", schoolId);
                // 不是最后一所学校，则sleep5分钟再处理下一所，错开压力
                if (i != schoolIdList.size() - 1) {
                    log.info("【作业定时收卷】等待5分钟后处理下一所学校");
                    Thread.sleep(5 * 60 * 1000);
                }
            }
        } catch (Exception e) {
            log.error("【作业定时收卷】全局扫描整体异常，流程中断", e);
        }
    }

    /**
     * 单作业超时自动收卷（优化：单学生独立事务，互不影响）
     */
    /**
     * 重载：同作业所有学生共用一条事务连接，不重复创建连接
     */
    public Result autoTimeoutCollectWork(int schoolId, Integer workId, SlSchool slSchool) throws Exception {
        long nowSec = System.currentTimeMillis() / 1000;
        Connection queryConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        List<Map<String, Integer>> timeoutRecordList = new ArrayList<>();

        try {
            String queryTimeoutSql = """
        SELECT r.id AS recordId, r.userId, r.courseId
        FROM yee_work_record r
        INNER JOIN yee_work w ON r.workId = w.id AND r.schoolId = w.schoolId
        WHERE r.schoolId = ? AND r.workId = ? AND r.state = 1 AND ? > w.endTime
        """;
            PreparedStatement pstQuery = queryConn.prepareStatement(queryTimeoutSql);
            pstQuery.setInt(1, schoolId);
            pstQuery.setInt(2, workId);
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
            return Result.success("暂无超时未提交学生");
        }

        int successCount = 0;
        List<Integer> failUserIds = new ArrayList<>();
        Map<Integer, String> failMsg = new HashMap<>();

        // 优化点：整个作业只创建1条事务连接，所有学生复用
        Connection transConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        transConn.setAutoCommit(false);
        try {
            for (Map<String, Integer> item : timeoutRecordList) {
                Integer recordId = item.get("recordId");
                Integer userId = item.get("userId");
                Integer courseId = item.get("courseId");
                try {
                    // 共用同一条连接，不再新建
                    doFinishWorkTransaction(transConn, schoolId, recordId, workId, userId, courseId, 3);
                    successCount++;
                } catch (SQLException e) {
                    failUserIds.add(userId);
                    String errLog = "recordId:" + recordId + ",err:" + e.getMessage();
                    failMsg.put(userId, errLog);
                    log.warn("【作业单学生收卷失败】schoolId={},workId={},userId={},msg={}", schoolId, workId, userId, errLog);
                }
            }
            transConn.commit();
        } catch (Exception e) {
            transConn.rollback();
            log.error("【作业批量收卷整体事务回滚 schoolId={},workId={}", schoolId, workId, e);
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

    // 保留原有无SlSchool入参的兼容方法，上层调用不用改
    public Result autoTimeoutCollectWork(int schoolId, Integer workId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        return autoTimeoutCollectWork(schoolId, workId, slSchool);
    }

    /**
     * 查询所有启用学校 + 10分钟内到期作业
     */
    private List<Map<String, Integer>> getAllSchoolWorkList() throws Exception {
        List<Map<String, Integer>> finalList = new ArrayList<>();
        long nowSec = System.currentTimeMillis() / 1000;
        long cycleSec = 600;
//        long monitorMaxTime = nowSec + cycleSec;
        long monitorMaxTime = nowSec;

        LambdaQueryWrapper<SlSchool> schoolWrapper = new LambdaQueryWrapper<>();
        schoolWrapper.eq(SlSchool::getAllow, 1);
        List<SlSchool> openSchoolList = slSchoolMapper.selectList(schoolWrapper);

        for (SlSchool school : openSchoolList) {
            Integer schoolId = school.getId();
            Connection conn = SlaveMysqlConnectionUtil.getConnection(school);
            // 筛选结束时间≤当前+10分钟的作业
            String sql = """
        SELECT DISTINCT id AS workId 
        FROM yee_work 
        WHERE schoolId = ? AND endTime <= ?
        """;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, schoolId);
            pst.setLong(2, monitorMaxTime);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Map<String, Integer> map = new HashMap<>();
                map.put("schoolId", schoolId);
                map.put("workId", rs.getInt("workId"));
                finalList.add(map);
            }
            closeResultSetAndStatement(rs, pst);
            closeConnection(conn);
        }
        return finalList;
    }
    /**
     * 统一作业交卷内部事务方法（教师强制/定时自动/手动提交共用）
     * @param conn 外部传入主库事务连接
     * @param schoolId 学校ID
     * @param recordId 作业作答记录ID
     * @param workId 作业ID
     * @param userId 学生ID
     * @param courseId 课程ID
     * @param submitType 1学生手动/2教师批量强制/3定时超时自动
     */
    private void doFinishWorkTransaction(Connection conn, int schoolId, Integer recordId, Integer workId, Integer userId, Integer courseId, int submitType) throws Exception {
        // 1、查询本场所有作业题目
        List<Map<String, Object>> maps = queryPaperTopicsAsMap(schoolId, workId, userId, recordId);
        if (maps == null || maps.isEmpty()) {
            throw new SQLException("recordId:" + recordId + " 无作业题目，无法执行收卷操作");
        }

        // 2、查询该学生本场所有作答记录
        List<YeeWorkAnswer> yeeWorkAnswers = queryYeeWorkAnswers(schoolId, recordId, workId, userId, courseId);
        List<Map<String, Object>> answerMapList = yeeWorkAnswers.stream()
                .map(ans -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("topicId", ans.getTopicId());
                    map.put("answer", ans.getAnswer());
                    map.put("images", ans.getImages());
                    map.put("files", ans.getFiles());
                    return map;
                }).collect(Collectors.toList());

        // 3、客观题自动算分
        List<Map<String, Object>> scoredResult = ScoreCalculator.calculateAnswerScores(maps, answerMapList);

        // 判断是否存在主观题(type=4)，区分最终状态：2待批阅 / 3已完成自动打分
        boolean hasSubjective = maps.stream()
                .anyMatch(m -> "4".equals(String.valueOf(m.get("type"))));
        int finalState = hasSubjective ? 2 : 3;
        long timeSec = System.currentTimeMillis() / 1000;

        // 4、批量更新作答得分【此处修正参数顺序】
        updateAnswerScores(conn, schoolId, scoredResult);

        // 5、直接手写更新yee_work_record SQL，无重载方法
        String updateRecordSql = """
            UPDATE yee_work_record 
            SET state = ?, 
                finishTime = ?, 
                score = ?, 
                frequency = frequency + 1, 
                markTime = ?, 
                obScore = ?,
                submitType = ?,
                submitTime = ?
            WHERE id = ?
              AND workId = ?
              AND userId = ?
              AND courseId = ?
              AND schoolId = ?
              AND state = 1
            """;
        PreparedStatement pstRecord = conn.prepareStatement(updateRecordSql);
        BigDecimal totalEarned = scoredResult.stream()
                .map(item -> Optional.ofNullable((BigDecimal) item.get("earnedScore")).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int idx = 1;
        pstRecord.setInt(idx++, finalState);
        pstRecord.setInt(idx++, (int) timeSec);
        pstRecord.setBigDecimal(idx++, totalEarned);
        pstRecord.setInt(idx++, (int) timeSec);
        pstRecord.setBigDecimal(idx++, totalEarned);
        pstRecord.setInt(idx++, submitType);
        pstRecord.setInt(idx++, (int) timeSec);
        pstRecord.setInt(idx++, recordId);
        pstRecord.setInt(idx++, workId);
        pstRecord.setInt(idx++, userId);
        pstRecord.setInt(idx++, courseId);
        pstRecord.setInt(idx, schoolId);

        int affectRow = pstRecord.executeUpdate();
        closeStatement(pstRecord);
        if (affectRow <= 0) {
            throw new SQLException("学生作业已完成提交，无需重复操作");
        }

        // 6、更新课程完成统计、作业总分统计表
        updateWorkCountForCourse(schoolId, courseId, userId);
        updateYeeWorkScore(workId, userId, courseId, schoolId, scoredResult, finalState);
    }
}
