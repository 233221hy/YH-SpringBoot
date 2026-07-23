package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.xfywz.guozespring.entity.dto.*;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeCourse;
import cn.xfywz.guozespring.entity.mhsch.YeeNode;
import cn.xfywz.guozespring.entity.mhsch.YeeStudyTotal;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.YeeStudyTotalService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.util.TimeFormatUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class YeeStudyTotalServiceImpl implements YeeStudyTotalService {

    // 建议自定义线程池，避免抢占公共异步池，放在类成员
    private final ExecutorService sessionAutoEndExecutor = Executors.newFixedThreadPool(10);

    private static final Logger log = LoggerFactory.getLogger(YeeStudyTotalServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 基础常量
    private static final int MIN_HEARTBEAT_INTERVAL = 1; // 最小心跳间隔（秒）
    public static final String SESSION_HASH_KEY_PREFIX = "study_session:";
    private static final String USER_NODE_SESSION_KEY = "user_node_session:";
    private static final String SESSION_ZSET_KEY = "active_sessions";
    private static final int SESSION_TIMEOUT_SECONDS = 600;
    private static final long TIME_TOLERANCE_SECONDS = 10L; // 时间容差（秒）
    private static final double COMPLETION_THRESHOLD = 0.97; // 97%完成阈值

    private static final double MAX_BACKWARD_DELTA = 0.8; // 最大允许回退比例
    private static final double MAX_FORWARD_DELTA = 0.6; // 最大允许前进比例
    private static final String SESSION_END_FLAG_PREFIX = "study:end:flag:";
    private static final long END_FLAG_EXPIRE_HOUR = 2;

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveStudyRecord(int schoolId, int userId, int nodeId, int courseId,
                                  int duration, String progress, String ip, String terminal) throws Exception {
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            conn.setAutoCommit(false);
            boolean isSuccess = false;
            Map<String, Object> resultData = new HashMap<>();
            String resultMsg = "";

            try {
                // 2. 查询视频节点信息和历史学习记录
                String nodeInfoSql = "SELECT n.videoDuration, st.* FROM yee_node n " +
                        "LEFT JOIN yee_study_total st ON n.id = st.nodeId AND st.userId = ? AND st.schoolId = ? " +
                        "WHERE n.id = ? AND n.schoolId = ? FOR UPDATE"; // 加行锁

                YeeStudyTotal existingRecord = null;
                long videoDuration = 0;

                try (PreparedStatement nodeStmt = conn.prepareStatement(nodeInfoSql)) {
                    nodeStmt.setInt(1, userId);
                    nodeStmt.setInt(2, schoolId);
                    nodeStmt.setInt(3, nodeId);
                    nodeStmt.setInt(4, schoolId);

                    try (ResultSet rs = nodeStmt.executeQuery()) {
                        if (rs.next()) {
                            videoDuration = rs.getLong("videoDuration");

                            if (rs.getLong("id") > 0) {
                                existingRecord = new YeeStudyTotal();
                                existingRecord.setId(rs.getLong("id"));
                                existingRecord.setNodeId(rs.getLong("nodeId"));
                                existingRecord.setUserId(rs.getLong("userId"));
                                existingRecord.setDuration(rs.getLong("duration"));
                                existingRecord.setProgress(rs.getString("progress"));
                                existingRecord.setCourseId(rs.getLong("courseId"));
                                existingRecord.setState(rs.getInt("state"));
                                existingRecord.setTimes(rs.getLong("times"));
                                existingRecord.setFinalTime(rs.getLong("finalTime"));
                                existingRecord.setSchoolId(rs.getLong("schoolId"));
                            }
                        } else {
                            return Result.error("未找到对应的视频节点");
                        }
                    }
                }

                long currentTime = System.currentTimeMillis() / 1000;
                Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

                // 3. 计算本次有效时长
                int actualDuration = duration;
                long historicalDuration = existingRecord != null ? existingRecord.getDuration() : 0;

                if (videoDuration > 0) {
                    double frontProgress = parseProgress(progress); // 前端传入的0-100百分比
                    // 100%进度直接补满剩余时长，跳过时间校验
                    if (frontProgress >= 100.0) {
                        long remainingDuration = videoDuration - historicalDuration;
                        if (remainingDuration > 0) {
                            actualDuration = (int) remainingDuration;
                        }
                    }
                }

                // 4. 计算总时长并应用97%阈值规则
//                long newTotalDuration = historicalDuration + actualDuration;
//                int calculatedState = 0;

//                if (videoDuration > 0) {
//                    // 用BigDecimal避免浮点精度丢失，保留4位小数
//                    BigDecimal ratio = BigDecimal.valueOf(newTotalDuration)
//                            .divide(BigDecimal.valueOf(videoDuration), 4, RoundingMode.HALF_UP);
//                    boolean reachThreshold = ratio.compareTo(BigDecimal.valueOf(COMPLETION_THRESHOLD)) >= 0;
//
//                    if (reachThreshold && ratio.compareTo(BigDecimal.ONE) < 0) {
//                        newTotalDuration = videoDuration;
//                        calculatedState = 1;
//                    } else if (ratio.compareTo(BigDecimal.ONE) >= 0) {
//                        calculatedState = 1;
//                    } else {
//                        calculatedState = 0;
//                    }
//                }
                // 4. 完成判定规则：前端心跳进度 >= 97% 才算完成（防刷课）
                long newTotalDuration = historicalDuration + actualDuration;
                int calculatedState = 0;

// 前端传入的进度（0-100）
                double frontProgress = parseProgress(progress);

                if (videoDuration > 0) {
                    // ============== 唯一修改：按前端进度判断 ==============
                    boolean reachThreshold = frontProgress >= 97.0;

                    if (reachThreshold) {
                        newTotalDuration = videoDuration;
                        calculatedState = 1;
                    } else {
                        calculatedState = 0;
                    }
                }

                if (calculatedState == 1 && videoDuration > 0 && historicalDuration < videoDuration) {
                    actualDuration = (int) (videoDuration - historicalDuration);
                    actualDuration = Math.max(actualDuration, 0); // 防止负数
                }

                // 5. 计算数据库存储的进度（前端百分比→0-1.00小数）
                String calculatedProgress;
                if (videoDuration > 0) {
                    double progressRate = Math.min((double) newTotalDuration / videoDuration * 100, 100.0);
                    calculatedProgress = front2DbProgress(progressRate); // 核心转换
                } else {
                    // 异常视频时长，进度固定为0.00
                    calculatedProgress = "0.00";
                }

                // 6. 插入学习明细（yee_study_time）：记录实际有效时长
                String insertStudyTimeSql = "INSERT INTO yee_study_time (nodeId, userId, duration, addTime, " +
                        "ip, terminal, courseId, beginTime, lastTime, schoolId, post, close, wg) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement studyTimeStmt = conn.prepareStatement(insertStudyTimeSql)) {
                    studyTimeStmt.setInt(1, nodeId);
                    studyTimeStmt.setInt(2, userId);
                    studyTimeStmt.setInt(3, actualDuration);
                    studyTimeStmt.setTimestamp(4, currentTimestamp);
                    studyTimeStmt.setString(5, ip != null ? ip : "");
                    studyTimeStmt.setString(6, terminal != null ? terminal : "web");
                    studyTimeStmt.setInt(7, courseId);
                    studyTimeStmt.setLong(8, currentTime);
                    studyTimeStmt.setLong(9, currentTime);
                    studyTimeStmt.setInt(10, schoolId);
                    studyTimeStmt.setInt(11, 1);
                    studyTimeStmt.setInt(12, 1);
                    studyTimeStmt.setInt(13, 0);

                    studyTimeStmt.executeUpdate();
                }

                // 7. 更新/插入汇总表
                if (existingRecord != null) {
                    String updateSql = "UPDATE yee_study_total SET duration = ?, progress = ?, state = ?, " +
                            "times = times + 1, finalTime = ? WHERE id = ?";

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, newTotalDuration);
                        updateStmt.setString(2, calculatedProgress); // 存入0-1.00小数
                        updateStmt.setInt(3, calculatedState);
                        updateStmt.setLong(4, currentTime);
                        updateStmt.setLong(5, existingRecord.getId());

                        int result = updateStmt.executeUpdate();
                        if (result > 0) {
                            isSuccess = true;
                            resultMsg = "学习记录更新成功";
                            resultData.put("nodeId", nodeId);
                            resultData.put("userId", userId);
                            resultData.put("duration", newTotalDuration);
                            resultData.put("progress", calculatedProgress); // 返回小数格式
                            resultData.put("progressPercent", db2FrontProgress(calculatedProgress)); // 兼容前端返回百分比
                            resultData.put("state", calculatedState);
                            resultData.put("times", existingRecord.getTimes() + 1);
                            resultData.put("videoDuration", videoDuration);
                        } else {
                            resultMsg = "学习记录更新失败";
                        }
                    }
                } else {
                    String insertSql = "INSERT INTO yee_study_total (nodeId, userId, duration, progress, courseId, " +
                            "state, times, finalTime, schoolId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        insertStmt.setInt(1, nodeId);
                        insertStmt.setInt(2, userId);
                        insertStmt.setLong(3, newTotalDuration);
                        insertStmt.setString(4, calculatedProgress); // 存入0-1.00小数
                        insertStmt.setInt(5, courseId);
                        insertStmt.setInt(6, calculatedState);
                        insertStmt.setInt(7, 1);
                        insertStmt.setLong(8, currentTime);
                        insertStmt.setInt(9, schoolId);

                        int result = insertStmt.executeUpdate();
                        if (result > 0) {
                            isSuccess = true;
                            resultMsg = "学习记录保存成功";
                            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                                long newId = 0;
                                if (generatedKeys.next()) {
                                    newId = generatedKeys.getLong(1);
                                }
                                resultData.put("id", newId);
                                resultData.put("nodeId", nodeId);
                                resultData.put("userId", userId);
                                resultData.put("duration", newTotalDuration);
                                resultData.put("progress", calculatedProgress); // 返回小数格式
                                resultData.put("progressPercent", db2FrontProgress(calculatedProgress)); // 兼容前端返回百分比
                                resultData.put("state", calculatedState);
                                resultData.put("times", 1);
                                resultData.put("videoDuration", videoDuration);
                            }
                        } else {
                            resultMsg = "学习记录保存失败";
                        }
                    }
                }

                // 统一提交/回滚
                if (isSuccess) {
                    conn.commit();
                    // 返回标准JSON格式，而非toString()
                    String jsonData = OBJECT_MAPPER.writeValueAsString(resultData);
                    return Result.success(jsonData, resultMsg);
                } else {
                    conn.rollback();
                    return Result.error(resultMsg);
                }
            } catch (SQLException e) {
                // 确保rollback异常不影响finally执行
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                }
                throw e;
            } finally {
                // 确保AutoCommit恢复，捕获异常
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                }
            }

        } catch (SQLException e) {
            return Result.error("数据库操作失败：" + e.getMessage());
        } catch (JsonProcessingException e) {
            return Result.error("返回数据格式化失败");
        } catch (Exception e) {
            return Result.error("保存学习记录失败：" + e.getMessage());
        }
    }

    @Override
    public Result getStudyProgress(int schoolId, int userId, int courseId) {
        try {
            SlSchool school = slSchoolMapper.selectById(schoolId);
            if (school == null || school.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            try (Connection conn = SlaveMysqlConnectionUtil.getConnection(school)) {
                String sql = """
            SELECT 
                c.name AS chapterName,
                n.id AS nodeId,
                n.name AS nodeName,
                n.videoDuration,
                COALESCE(SUM(st.duration), 0) AS watchDuration,
                COALESCE(MAX(srt.duration), 0) AS totalDbDuration,
                COUNT(st.id) AS watchCount,
                MIN(st.beginTime) AS startTime,
                MAX(st.lastTime) AS completeTime,
                COALESCE(MAX(srt.progress), '0.00') AS dbProgress,
                COALESCE(MAX(srt.state), 0) AS totalState
            FROM yee_node n
            LEFT JOIN yee_chapter c ON n.chapterId = c.id
            LEFT JOIN yee_study_time st 
                ON st.nodeId = n.id 
                AND st.userId = ?
                AND st.schoolId = ?
                AND st.courseId = ?
            LEFT JOIN yee_study_total srt
                ON srt.nodeId = n.id
                AND srt.userId = ?
                AND srt.schoolId = ?
                AND srt.courseId = ?
            WHERE n.courseId = ? 
              AND n.schoolId = ? 
              AND n.tabVideo = 1
            GROUP BY 
                n.id, c.id, c.name, n.name, n.videoDuration
            ORDER BY 
                IFNULL(c.sort, 999999) ASC,
                c.id ASC,
                IFNULL(n.sort, 999999) ASC,
                n.id ASC
            """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, schoolId);
                    stmt.setInt(3, courseId);
                    stmt.setInt(4, userId);
                    stmt.setInt(5, schoolId);
                    stmt.setInt(6, courseId);
                    stmt.setInt(7, courseId);
                    stmt.setInt(8, schoolId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Map<String, Object>> list = new ArrayList<>();
                        int totalNodes = 0;
                        int completedNodes = 0;
                        long totalWatchSeconds = 0;
                        long totalVideoSeconds = 0;

                        while (rs.next()) {
                            long nodeId = rs.getLong("nodeId");
                            long videoDur = rs.getLong("videoDuration");
                            long watchDur = rs.getLong("watchDuration");
                            long totalDbDur = rs.getLong("totalDbDuration");
                            int watchCount = rs.getInt("watchCount");
                            String dbProgress = rs.getString("dbProgress");
                            int dbState = rs.getInt("totalState");

                            // 【修改核心】完全移除时长兜底判断，仅以yee_study_total.state作为唯一标准
                            boolean isCompleted;
                            if (watchCount == 0) {
                                isCompleted = false;
                            } else {
                                // 只判断汇总表state=1，不再判断时长
                                isCompleted = (dbState == 1);
                            }

                            // 打印状态不一致告警日志（用于排查脏数据）
                            boolean dbMarkFinish = dbState == 1;
                            if (isCompleted != dbMarkFinish) {
                                log.warn("进度数据不一致 school:{},user:{},course:{},node:{},实时完成:{},total表标记:{}",
                                        schoolId, userId, courseId, nodeId, isCompleted, dbMarkFinish);
                            }

                            // 时间戳转Timestamp
                            Object startTimeObj = rs.getObject("startTime");
                            Object completeTimeObj = rs.getObject("completeTime");
                            Timestamp startTime = null;
                            Timestamp completeTime = null;
                            if (startTimeObj != null) {
                                startTime = new Timestamp(((Number) startTimeObj).longValue() * 1000L);
                            }
                            if (completeTimeObj != null) {
                                completeTime = new Timestamp(((Number) completeTimeObj).longValue() * 1000L);
                            }

                            // 进度百分比计算
                            double progress = 0.0;
                            if (StringUtils.isNotBlank(dbProgress)) {
                                progress = db2FrontProgress(dbProgress);
                            } else if (videoDur > 0) {
                                progress = Math.min(watchDur * 100.0 / videoDur, 100.0);
                            }

                            // 展示时长优先取汇总表补齐时长
                            long showWatchDur = dbState == 1 ? Math.max(totalDbDur, watchDur) : watchDur;

                            // 状态文本
                            String statusText = isCompleted ? "已完成" : (watchCount > 0 ? "学习中" : "未开始");

                            Map<String, Object> nodeProgress = new HashMap<>();
                            nodeProgress.put("chapterName", rs.getString("chapterName"));
                            nodeProgress.put("nodeId", nodeId);
                            nodeProgress.put("nodeName", rs.getString("nodeName"));

                            try {
                                nodeProgress.put("videoDuration", TimeFormatUtil.formatDuration(videoDur));
                                nodeProgress.put("watchDuration", TimeFormatUtil.formatDuration(showWatchDur));
                            } catch (Exception e) {
                                nodeProgress.put("videoDuration", "00:00:00");
                                nodeProgress.put("watchDuration", "00:00:00");
                            }

                            nodeProgress.put("watchCount", watchCount);
                            nodeProgress.put("state", isCompleted ? 1 : 0);
                            nodeProgress.put("statusText", statusText);
                            nodeProgress.put("progressPercent", Math.round(progress));
                            nodeProgress.put("progressRatio", dbProgress != null ? dbProgress : "0.00");
                            nodeProgress.put("startTime", startTime);
                            nodeProgress.put("completeTime", completeTime);

                            list.add(nodeProgress);
                            totalNodes++;
                            totalWatchSeconds += watchDur;
                            totalVideoSeconds += videoDur;
                            if (isCompleted) {
                                completedNodes++;
                            }
                        }

                        // 汇总完成率
                        double completionRate = totalNodes > 0
                                ? Math.round(completedNodes * 10000.0 / totalNodes) / 100.0
                                : 0.0;

                        Map<String, Object> summary = new HashMap<>();
                        summary.put("totalNodes", totalNodes);
                        summary.put("completedNodes", completedNodes);
                        try {
                            summary.put("totalWatchDuration", TimeFormatUtil.formatDuration(totalWatchSeconds));
                            summary.put("totalVideoDuration", TimeFormatUtil.formatDuration(totalVideoSeconds));
                        } catch (Exception e) {
                            summary.put("totalWatchDuration", "00:00:00");
                            summary.put("totalVideoDuration", "00:00:00");
                        }
                        summary.put("completionRate", completionRate);

                        Map<String, Object> result = new HashMap<>();
                        result.put("summary", summary);
                        result.put("nodeProgressList", list);

                        return Result.success(result);
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询学习进度失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result startSession(StudySessionStartDTO dto, String clientIp) {
        // 1. 校验参数合法性
        if (dto.getUserId() <= 0 || dto.getNodeId() <= 0 || dto.getCourseId() <= 0 || dto.getSchoolId() <= 0) {
            return Result.error("用户ID/节点ID/课程ID/学校ID不能为空且大于0");
        }

        // 2. 校验学校是否存在且已审核
        SlSchool school = slSchoolMapper.selectById(dto.getSchoolId());
        if (school == null || school.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 批量查询：选课校验、节点、课程、历史进度（共用一条连接）
        YeeNode node;
        int startProgressInt = 0;
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(school)) {
            String checkSelectSql = """
                SELECT 1 FROM yee_course_student
                WHERE studentId = ? AND courseId = ? AND schoolId = ?
                LIMIT 1
                """;
            boolean hasSelectRecord = false;
            try (PreparedStatement ps = conn.prepareStatement(checkSelectSql)) {
                ps.setInt(1, dto.getUserId());
                ps.setInt(2, dto.getCourseId());
                ps.setInt(3, dto.getSchoolId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hasSelectRecord = true;
                    }
                }
            }
            if (!hasSelectRecord) {
                log.warn("用户未选课禁止开启学习会话 userId:{},courseId:{},schoolId:{}",
                        dto.getUserId(), dto.getCourseId(), dto.getSchoolId());
                return Result.error("未选该课程，无法开始学习");
            }

            // 3.1 查询视频节点
            node = selectYeeNode(conn, dto.getNodeId(), dto.getSchoolId());
            if (node == null) {
                return Result.error("视频节点不存在");
            }
            // 3.2 校验课程是否已开课
            YeeCourse course = selectYeeCourse(conn, dto.getCourseId());
            if (course != null && course.getStartDate() != null && course.getStartDate().after(new java.util.Date())) {
                return Result.error("课程尚未开课");
            }
            // 3.3 读取用户历史进度
            YeeStudyTotal existing = selectStudyTotal(conn, dto.getUserId(), dto.getNodeId(), dto.getSchoolId());
            if (existing != null && StringUtils.isNotBlank(existing.getProgress())) {
                startProgressInt = db2FrontProgress(existing.getProgress());
            }
        } catch (Exception e) {
            log.error("开启会话查询数据库异常", e);
            return Result.error("系统错误");
        }
        String safeStartProgress = String.valueOf(startProgressInt);

        // 5. 单会话控制：检查是否已有活跃会话（userId + nodeId 维度）
        String userNodeKey = USER_NODE_SESSION_KEY + dto.getUserId() + ":" + dto.getNodeId();
        String existingSessionId = (String) redisTemplate.opsForValue().get(userNodeKey);

        if (existingSessionId != null) {
            String existingHashKey = SESSION_HASH_KEY_PREFIX + existingSessionId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(existingHashKey))) {
                try {
                    endSessionInternal(existingSessionId);
                } catch (Exception e) {
                    log.warn("强制结束旧会话 {} 失败，继续创建新会话", existingSessionId, e);
                }
            }
        }

        // 6. 生成新的纯 UUID sessionId
        String sessionId = UUID.randomUUID().toString();
        String hashKey = SESSION_HASH_KEY_PREFIX + sessionId;
        long nowSec = System.currentTimeMillis() / 1000;

        // 7. 构建 Redis 存储数据
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("userId", String.valueOf(dto.getUserId()));
        sessionData.put("nodeId", String.valueOf(dto.getNodeId()));
        sessionData.put("courseId", String.valueOf(dto.getCourseId()));
        sessionData.put("schoolId", String.valueOf(dto.getSchoolId()));
        sessionData.put("startTime", String.valueOf(nowSec));
        sessionData.put("lastActive", String.valueOf(nowSec));
        sessionData.put("ip", StringUtils.defaultString(clientIp, "unknown"));
        sessionData.put("terminal", StringUtils.defaultIfBlank(dto.getTerminal(), "web"));
        sessionData.put("startProgress", safeStartProgress); // 前端百分比格式
        sessionData.put("currentProgress", safeStartProgress); // 前端百分比格式
        sessionData.put("videoDuration", String.valueOf(node.getVideoDuration()));

        // 8. 原子性写入 Redis
        try {
            Boolean executeResult = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                try {
                    connection.multi(); // 开启Redis事务

                    // 8.1 写入会话哈希
                    Map<byte[], byte[]> hashBytes = new HashMap<>();
                    for (Map.Entry<String, String> entry : sessionData.entrySet()) {
                        hashBytes.put(entry.getKey().getBytes(), entry.getValue().getBytes());
                    }
                    connection.hMSet(hashKey.getBytes(), hashBytes);
                    connection.expire(hashKey.getBytes(), SESSION_TIMEOUT_SECONDS + 60);

                    // 8.2 加入活跃会话有序集合
                    connection.zAdd(SESSION_ZSET_KEY.getBytes(), nowSec, sessionId.getBytes());

                    // 8.3 更新用户-节点维度的当前会话
                    connection.setEx(userNodeKey.getBytes(), SESSION_TIMEOUT_SECONDS + 60, sessionId.getBytes());

                    // 执行事务
                    List<Object> results = connection.exec();
                    // 检查事务执行结果
                    return results != null && !results.isEmpty() && results.stream().allMatch(Objects::nonNull);
                } catch (Exception e) {
                    connection.discard(); // 回滚事务
                    return false;
                }
            });

            if (Boolean.TRUE.equals(executeResult)) {
                return Result.success("会话已开始", sessionId);
            } else {
                throw new RuntimeException("Redis原子操作执行失败");
            }

        } catch (Exception e) {
            // 尝试清理可能残留的数据
            redisTemplate.delete(hashKey);
            redisTemplate.opsForZSet().remove(SESSION_ZSET_KEY, sessionId);
            log.error("创建Redis学习会话失败", e);
            return Result.error("会话创建失败，请稍后重试");
        }
    }

    @Override
    public Result heartbeat(StudySessionHeartbeatDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Result.error("sessionId 不能为空");
        }

        String hashKey = SESSION_HASH_KEY_PREFIX + sessionId;
        if (!redisTemplate.hasKey(hashKey)) {
            return Result.error("会话不存在或已过期");
        }

        // 读取Redis中的会话数据（前端百分比格式）
        String oldProgStr = getStringFromHash(hashKey, "currentProgress", "0");
        String startProgStr = getStringFromHash(hashKey, "startProgress", "0");
        String videoDurationStr = getStringFromHash(hashKey, "videoDuration", "0");
        String lastActiveStr = getStringFromHash(hashKey, "lastActive", "0");

        double oldFrontProg = parseProgress(oldProgStr); // 前端百分比（如50.0）
        double startFrontProg = parseProgress(startProgStr); // 前端百分比（如0.0）
        long videoDuration;
        try {
            videoDuration = Long.parseLong(videoDurationStr);
        } catch (NumberFormatException e) {
            videoDuration = 0;
        }
        long lastActiveTime = Long.parseLong(lastActiveStr);
        long nowSec = System.currentTimeMillis() / 1000;

        // 心跳频率限制（防刷）
        if (nowSec - lastActiveTime < MIN_HEARTBEAT_INTERVAL) {
            return Result.error("心跳过于频繁，请间隔" + MIN_HEARTBEAT_INTERVAL + "秒后再试");
        }

        // 解析前端传入的进度（0-100百分比）
        double newFrontProg;
        try {
            newFrontProg = parseProgress(dto.getProgress());
        } catch (Exception e) {
            return Result.error("进度格式无效");
        }

        // 转换为0-1小数进行校验（核心：统一用小数做业务判断）
        double oldDecimalProg = oldFrontProg / 100.0;
        double newDecimalProg = newFrontProg / 100.0;
        // 限定范围0-1，防止异常值
        newDecimalProg = Math.min(Math.max(newDecimalProg, 0.0), 1.0);

        // 计算进度增量（基于小数）
        double progressDeltaFromStart = newDecimalProg - (startFrontProg / 100.0);
        long minRequiredSeconds = 0;
        if (progressDeltaFromStart > 0 && videoDuration > 0) {
            minRequiredSeconds = (long) Math.ceil(progressDeltaFromStart * videoDuration);
        }
        long actualSessionTime = nowSec - getLongFromHash(hashKey, "startTime", nowSec);

        // 仅当未完成时校验时间
        if (progressDeltaFromStart > 0 && newDecimalProg < 1.0) {
            if (actualSessionTime + TIME_TOLERANCE_SECONDS < minRequiredSeconds) {
                return Result.error("学习进度异常：观看时间不足");
            }
        }

        // 非首次更新：检测异常跳跃/回退（基于小数）
        boolean isFirstUpdate = (oldFrontProg == startFrontProg);
        if (!isFirstUpdate) {
            double backwardDelta = oldDecimalProg - newDecimalProg;
            // 允许最多回退 80%（0.8小数）
            if (backwardDelta > MAX_BACKWARD_DELTA) {
                return Result.error("学习进度异常：回退幅度超出限制");
            }

            // 允许最多前进 40%（0.4小数），但100%（已完成）跳过校验
            if (newDecimalProg != 1.0 && newDecimalProg > oldDecimalProg + MAX_FORWARD_DELTA) {
                return Result.error("学习进度异常：前进幅度超出限制（单次最多前进60%）");
            }
        }

        // 更新 Redis：
        // 1. lastActive 刷新为当前时间
        // 2. currentProgress 仍存储前端百分比格式（供前端后续使用）
        // 3. 后续入库时通过 front2DbProgress 转换为0-1.00小数
        redisTemplate.opsForHash().put(hashKey, "lastActive", String.valueOf(nowSec));
        redisTemplate.opsForHash().put(hashKey, "currentProgress", String.format("%.1f", newFrontProg));
        redisTemplate.expire(hashKey, SESSION_TIMEOUT_SECONDS + 60, TimeUnit.SECONDS);
        redisTemplate.opsForZSet().add(SESSION_ZSET_KEY, sessionId, (double) nowSec);
        return Result.success("心跳成功");
    }
    @Override
    public Result endSession(StudySessionEndDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Result.error("无效会话ID");
        }

        String hashKey = SESSION_HASH_KEY_PREFIX + sessionId;
        String endFlagKey = SESSION_END_FLAG_PREFIX + sessionId;

        // 幂等拦截：已经执行过结束入库，直接返回，不走DB逻辑
        if (Boolean.TRUE.equals(redisTemplate.hasKey(endFlagKey))) {
            log.info("会话{} 重复结束请求，幂等拦截，不再执行入库", sessionId);
            return Result.success("会话已结束或不存在");
        }

        // 会话已过期直接返回
        if (!redisTemplate.hasKey(hashKey)) {
            return Result.success("会话已结束或不存在");
        }

        Map<Object, Object> sessionMap = redisTemplate.opsForHash().entries(hashKey);

        // 修复NPE
        Long startTimeWrap = getLong(sessionMap, "startTime");
        long nowSec = System.currentTimeMillis() / 1000;
        long startTime = startTimeWrap == null ? nowSec : startTimeWrap;
        long endTime = nowSec;
        if (endTime <= 0) endTime = nowSec;

        // 构建会话对象
        StudySession session = new StudySession();
        session.setUserId(getInteger(sessionMap, "userId"));
        session.setNodeId(getInteger(sessionMap, "nodeId"));
        session.setCourseId(getInteger(sessionMap, "courseId"));
        session.setSchoolId(getInteger(sessionMap, "schoolId"));
        session.setStartTime(startTime);
        session.setLastActive(endTime);
        session.setIp(getString(sessionMap, "ip"));
        session.setTerminal(getString(sessionMap, "terminal"));
        session.setStartProgress(getString(sessionMap, "startProgress"));

        if (endTime == session.getStartTime()) {
            endTime = nowSec;
            session.setLastActive(endTime);
        }

        // 安全处理 finalProgress
        String finalProgressToUse = getString(sessionMap, "currentProgress");
        if (finalProgressToUse == null || finalProgressToUse.trim().isEmpty()) {
            finalProgressToUse = "0";
        } else {
            finalProgressToUse = finalProgressToUse.trim();
        }
        session.setCurrentProgress(finalProgressToUse);

        // 仅校验核心参数非空
        boolean isSessionValid = true;
        if (session.getUserId() == null || session.getNodeId() == null || session.getSchoolId() == null) {
            isSessionValid = false;
        }

        if (!isSessionValid) {
            cleanupRedis(sessionId, hashKey);
            return Result.error("会话无效：核心参数缺失");
        }

        // 保存学习记录
        Result saveResult = saveStudyRecordFromSession(session, finalProgressToUse);
        // 清理 Redis
        cleanupRedis(sessionId, hashKey);

        return saveResult;
    }

    public Result saveStudyRecordFromSession(StudySession session, String finalProgress) {
        Integer schoolId = session.getSchoolId();
        if (schoolId == null) {
            return Result.error("学校ID不能为空");
        }

        SlSchool school = slSchoolMapper.selectById(schoolId);
        if (school == null) {
            return Result.error("学校不存在");
        }

        Connection conn = null;
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(school);
            conn.setAutoCommit(false);

            // 1. 获取视频节点信息
            YeeNode node = selectYeeNode(conn, session.getNodeId(), schoolId);
            if (node == null) {
                return Result.error("节点不存在");
            }
            long videoDuration = node.getVideoDuration(); // 单位：秒
            boolean isVideoDurationInvalid = videoDuration <= 0;
            long calcVideoDur = isVideoDurationInvalid ? 0 : videoDuration;

            // 2. 解析进度 [0.0, 100.0]
            double startP = parseProgress(session.getStartProgress());
            double endP = parseProgress(finalProgress);
            double thisFinalProgress = Math.min(Math.max(endP, 0.0), 100.0);

            // 3. 行锁查询历史汇总记录
            String selectTotalSql = """
                SELECT id, progress, duration, times, state
                FROM yee_study_total
                WHERE userId = ?
                  AND nodeId = ?
                  AND schoolId = ?
                  AND courseId = ?
                FOR UPDATE
            """;
            YeeStudyTotal existing = null;
            try (PreparedStatement pstmt = conn.prepareStatement(selectTotalSql)) {
                pstmt.setInt(1, session.getUserId());
                pstmt.setInt(2, session.getNodeId());
                pstmt.setInt(3, schoolId);
                pstmt.setInt(4, session.getCourseId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        existing = new YeeStudyTotal();
                        existing.setId(rs.getLong("id"));
                        existing.setProgress(rs.getString("progress"));
                        existing.setDuration(rs.getLong("duration"));
                        existing.setTimes(rs.getLong("times"));
                        existing.setState(rs.getInt("state"));
                    }
                }
            }

            double historicalProgress = 0.0;
            long historicalDuration = 0L;
            if (existing != null) {
                if (existing.getProgress() != null) {
                    historicalProgress = parseProgress(existing.getProgress());
                }
                historicalDuration = existing.getDuration();
            }

            long sessionDurationSec = Math.max(0, session.getLastActive() - session.getStartTime() + 1);
            long sessionActualDuration;
            // 统一阈值判断：97% / 100% 共用补齐逻辑，根治明细表差几秒
            boolean reachThreshold = thisFinalProgress >= 97.0;
            if ((thisFinalProgress >= 100.0 || reachThreshold) && !isVideoDurationInvalid) {
                long remainingDuration = videoDuration - historicalDuration;
                sessionActualDuration = Math.max(remainingDuration, 0);
            } else {
                // 未达标，记录真实会话观看时长
                sessionActualDuration = Math.min(sessionDurationSec, calcVideoDur);
            }

            // 5. 计算汇总表总时长、完成状态
            long newTotalDuration = historicalDuration + sessionActualDuration;
            int newState = 0;

            if (!isVideoDurationInvalid && calcVideoDur > 0) {
                if (reachThreshold) {
                    newTotalDuration = videoDuration;
                    newState = 1;
                    thisFinalProgress = 100.0;
                } else {
                    newState = 0;
                }
            } else {
                // 视频时长异常，保留原有完成状态
                newState = (existing != null && existing.getState() == 1) ? 1 : 0;
                thisFinalProgress = Math.max(thisFinalProgress, historicalProgress);
                newTotalDuration = historicalDuration + sessionActualDuration;
            }

            if (!isVideoDurationInvalid && newTotalDuration >= calcVideoDur) {
                newState = 1;
                thisFinalProgress = 100.0;
                long realTotalAdd = videoDuration - historicalDuration;
                sessionActualDuration = Math.max(realTotalAdd, sessionActualDuration);
            }

            // 6. 格式化进度字符串
            String newProgressStr;
            if (newState == 1) {
                newProgressStr = "1.00";
            } else {
                BigDecimal progressDecimal = BigDecimal.valueOf(thisFinalProgress)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                newProgressStr = progressDecimal.toPlainString();
            }

            // 7. 可选：限制总时长最多视频+10秒，放开注释启用
        if (!isVideoDurationInvalid) {
            newTotalDuration = Math.min(newTotalDuration, calcVideoDur + 10);
        }

            // 8. 插入/更新汇总表 yee_study_total
            if (existing != null) {
                updateStudyTotal(conn, existing.getId(), newTotalDuration, newProgressStr, newState);
            } else {
                insertStudyTotal(
                        conn,
                        session.getUserId(),
                        session.getNodeId(),
                        session.getCourseId(),
                        schoolId,
                        newTotalDuration,
                        newProgressStr,
                        newState
                );
            }

            // 9. 插入本次会话明细 yee_study_time（已补齐差额，总和与汇总表对齐）
            insertStudyTime(conn, session, (int) sessionActualDuration, session.getCourseId(), schoolId);

            // 10. 更新课程学生汇总
            int videoLearnedDelta = 0;
            if (existing != null && existing.getState() == 0 && newState == 1) {
                videoLearnedDelta = 1;
            } else if (existing == null && newState == 1) {
                videoLearnedDelta = 1;
            }
            try {
                upsertCourseStudent(conn, session.getUserId(), session.getCourseId(), schoolId,
                        videoLearnedDelta, sessionActualDuration);
            } catch (Exception e) {
                log.error("更新课程学生选课统计异常，视频学习数据不回滚", e);
            }

            conn.commit();
            return Result.success("学习记录已保存");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            log.error("保存学习记录异常", e);
            return Result.error("保存失败: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {}
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }


    @Override
    public Result getLastProgressStr(int userId, int nodeId, int schoolId) {
        // 1. 校验入参合法性
        if (userId <= 0 || nodeId <= 0 || schoolId <= 0) {
            return Result.success("0.00"); // 参数非法返回默认小数格式
        }

        // 2. 获取学校信息（用于确定连接哪个从库）
        SlSchool school = slSchoolMapper.selectById(schoolId);
        if (school == null || school.getAllow() == 0) {
            return Result.success("0.00"); // 学校异常返回默认小数格式
        }

        String sql = """
        SELECT progress
        FROM yee_study_total
        WHERE userId = ?
          AND nodeId = ?
          AND schoolId = ?  -- 补充schoolId条件，避免跨学校查询
        ORDER BY finalTime DESC
        LIMIT 1
        """;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(school);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置SQL参数
            stmt.setInt(1, userId);
            stmt.setInt(2, nodeId);
            stmt.setInt(3, schoolId);

            try (ResultSet rs = stmt.executeQuery()) {
                BigDecimal progressValue = BigDecimal.ZERO;
                if (rs.next()) {
                    String progressStr = rs.getString("progress");
                    if (StringUtils.isNotBlank(progressStr)) {
                        try {
                            // 解析为BigDecimal，兼容 "100"、"1.0"、"0.97" 等格式
                            BigDecimal rawProgress = new BigDecimal(progressStr.trim());
                            // 统一转换为 0-1.00 的小数
                            if (rawProgress.compareTo(BigDecimal.ONE) > 0 && rawProgress.compareTo(new BigDecimal(100)) <= 0) {
                                progressValue = rawProgress.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                            } else {
                                progressValue = rawProgress.setScale(2, RoundingMode.HALF_UP);
                            }
                        } catch (NumberFormatException e) {
                            progressValue = BigDecimal.ZERO;
                        }
                    }
                }

                // 边界值修正（确保最终值在 0-1.00 之间）
                if (progressValue.compareTo(BigDecimal.ZERO) < 0) {
                    progressValue = BigDecimal.ZERO;
                } else if (progressValue.compareTo(BigDecimal.ONE) > 0) {
                    progressValue = BigDecimal.ONE;
                }

                // 格式化输出（固定保留2位小数，如 1.0 → 1.00，0.97 → 0.97）
                String result = progressValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
                return Result.success(result);
            }
        } catch (SQLException e) {
            log.error("查询最后进度失败", e);
            return Result.success("0.00");
        } catch (Exception e) {
            log.error("查询最后进度异常", e);
            return Result.success("0.00");
        }
    }

    @Scheduled(fixedRate = 300000)
    public void autoEndExpiredSessions() {
        try {
            long now = System.currentTimeMillis() / 1000;
            // 超时阈值：无心跳超过SESSION_TIMEOUT_SECONDS判定为过期
            long threshold = now - SESSION_TIMEOUT_SECONDS;

            // 单次最多取100条过期会话，控制批量大小
            Set<String> expiredSessionIds = redisTemplate.opsForZSet()
                    .rangeByScore(SESSION_ZSET_KEY, 0, threshold, 0, 100);
            if (expiredSessionIds == null || expiredSessionIds.isEmpty()) {
                return;
            }
            log.info("定时任务检测到{}条超时学习会话，开始自动结算", expiredSessionIds.size());

            // 遍历过期会话，提交自定义线程池，限流防并发风暴
            for (String sessionId : expiredSessionIds) {
                sessionAutoEndExecutor.submit(() -> {
                    try {
                        String hashKey = SESSION_HASH_KEY_PREFIX + sessionId;
                        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(hashKey);
                        if (sessionData == null || sessionData.isEmpty()) {
                            // Redis会话数据已消失，仅清理zset残留
                            redisTemplate.opsForZSet().remove(SESSION_ZSET_KEY, sessionId);
                            return;
                        }

                        // 1. 封装会话对象，和手动endSession逻辑完全对齐
                        StudySession session = new StudySession();
                        session.setUserId(getInteger(sessionData, "userId"));
                        session.setNodeId(getInteger(sessionData, "nodeId"));
                        session.setCourseId(getInteger(sessionData, "courseId"));
                        session.setSchoolId(getInteger(sessionData, "schoolId"));
                        session.setIp(getString(sessionData, "ip"));
                        session.setTerminal(getString(sessionData, "terminal"));
                        session.setStartProgress(getString(sessionData, "startProgress"));

                        // 读取进度，兼容空值
                        String currentProgress = getString(sessionData, "currentProgress");
                        if (currentProgress == null || currentProgress.trim().isEmpty()) {
                            currentProgress = "0";
                        }
                        session.setCurrentProgress(currentProgress);

                        // 【核心修复1】结束时间以最后心跳lastActive为准，不用当前定时时间now
                        long startTime = getLongOrDefault(sessionData, "startTime", now);
                        long lastActive = getLongOrDefault(sessionData, "lastActive", now);
                        long endTime = lastActive;

                        // 限制最大时长不超过视频全长，防止挂机无限计时
                        long videoDuration = getLongOrDefault(sessionData, "videoDuration", 0);
                        if (videoDuration > 0) {
                            endTime = Math.min(endTime, startTime + videoDuration);
                        }
                        session.setStartTime(startTime);
                        session.setLastActive(endTime);

                        Integer userId = session.getUserId();
                        Integer nodeId = session.getNodeId();
                        boolean needCleanUserNodeLock = false;
                        String userNodeKey = null;
                        if (userId != null && userId > 0 && nodeId != null && nodeId > 0) {
                            userNodeKey = USER_NODE_SESSION_KEY + userId + ":" + nodeId;
                            String storedSessionId = (String) redisTemplate.opsForValue().get(userNodeKey);
                            // 只有当前锁绑定的是本次过期会话，才允许删除，防止误删新会话
                            if (storedSessionId != null && storedSessionId.equals(sessionId)) {
                                needCleanUserNodeLock = true;
                            }
                        }

                        // 2. 执行结算（和手动结束共用同一套save逻辑，规则统一）
                        Result saveResult = saveStudyRecordFromSession(session, currentProgress);
                        log.info("自动结算会话{}完成，结果:{}", sessionId, saveResult.getMsg());

                        // 3. 结算成功后统一清理Redis资源，避免中途新开会话被误删
                        if (needCleanUserNodeLock) {
                            redisTemplate.delete(userNodeKey);
                        }
                        // 删除会话hash与有序集合标记
                        redisTemplate.delete(hashKey);
                        redisTemplate.opsForZSet().remove(SESSION_ZSET_KEY, sessionId);

                    } catch (Exception ex) {
                        log.warn("定时任务自动处理超时会话{}异常", sessionId, ex);
                    }
                });
            }
        } catch (Exception e) {
            log.error("自动清理超时会话顶层异常", e);
        }
    }


    /**
     * 查询视频节点信息（含视频时长）
     */
    private YeeNode selectYeeNode(Connection conn, long nodeId, long schoolId) throws SQLException {
        // 校验入参
        if (nodeId <= 0 || schoolId <= 0) {
            return null;
        }

        String sql = "SELECT id, videoDuration FROM yee_node WHERE id = ? AND schoolId = ?";

        // 使用try-with-resources自动关闭PreparedStatement和ResultSet
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, nodeId);
            ps.setLong(2, schoolId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    YeeNode n = new YeeNode();
                    n.setId(rs.getLong("id"));
                    long videoDuration = rs.getLong("videoDuration");
                    n.setVideoDuration(videoDuration);

                    // 额外优化：处理视频时长异常值（≤0时设为0）
                    if (videoDuration <= 0) {
                        n.setVideoDuration(0);
                    }
                    return n;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw e; // 抛出异常由上层处理
        }
    }

    private YeeCourse selectYeeCourse(Connection conn, long courseId) throws SQLException {
        String sql = "SELECT id, startDate FROM yee_course WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    YeeCourse course = new YeeCourse();
                    course.setId(rs.getLong("id"));
                    course.setStartDate(rs.getDate("startDate"));
                    return course;
                }
            }
        }
        return null;
    }

    /**
     * 查询用户学习汇总记录
     */
    private YeeStudyTotal selectStudyTotal(Connection conn, long userId, long nodeId, long schoolId) throws SQLException {
        String sql = """
        SELECT id, nodeId, userId, duration, progress, courseId, state, times, finalTime, schoolId
        FROM yee_study_total 
        WHERE userId = ? AND nodeId = ? AND schoolId = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, nodeId);
            ps.setLong(3, schoolId);

            // ResultSet放在参数设置后，确保查询带参数执行
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    YeeStudyTotal t = new YeeStudyTotal();
                    t.setId(rs.getLong("id"));
                    t.setNodeId(rs.getLong("nodeId"));
                    t.setUserId(rs.getLong("userId"));
                    t.setDuration(rs.getLong("duration"));
                    t.setProgress(rs.getString("progress"));
                    t.setCourseId(rs.getLong("courseId"));
                    t.setState(rs.getInt("state"));
                    t.setTimes(rs.getLong("times"));
                    t.setFinalTime(rs.getLong("finalTime"));
                    t.setSchoolId(rs.getLong("schoolId"));
                    return t;
                }
            }
        }
        return null;
    }

    private void insertStudyTime(Connection conn, StudySession s, int duration, long courseId, long schoolId) throws SQLException {
        String sql = """
            INSERT INTO yee_study_time 
            (nodeId, userId, duration, addTime, ip, terminal, courseId, beginTime, lastTime, schoolId, post, close, wg)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, s.getNodeId());
            ps.setInt(2, s.getUserId());
            ps.setInt(3, duration);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setString(5, s.getIp());
            ps.setString(6, s.getTerminal());
            ps.setLong(7, courseId);
            ps.setLong(8, s.getStartTime());
            ps.setLong(9, s.getLastActive());
            ps.setLong(10, schoolId);
            ps.setInt(11, 1);
            ps.setInt(12, 1);
            ps.setInt(13, 0);
            ps.executeUpdate();
        }
    }

    private void updateStudyTotal(Connection conn, long id, long duration, String progress, int state) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE yee_study_total 
            SET duration = ?, progress = ?, state = ?, times = times + 1, finalTime = ?
            WHERE id = ?
            """)) {
            ps.setLong(1, duration);
            ps.setString(2, progress);
            ps.setInt(3, state);
            ps.setLong(4, System.currentTimeMillis() / 1000);
            ps.setLong(5, id);
            ps.executeUpdate();
        }
    }

    private void insertStudyTotal(Connection conn, long userId, long nodeId, long courseId, long schoolId, long duration, String progress, int state) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO yee_study_total 
            (nodeId, userId, duration, progress, courseId, state, times, finalTime, schoolId)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            ps.setLong(1, nodeId);
            ps.setLong(2, userId);
            ps.setLong(3, duration);
            ps.setString(4, progress);
            ps.setLong(5, courseId);
            ps.setInt(6, state);
            ps.setLong(7, 1);
            ps.setLong(8, System.currentTimeMillis() / 1000);
            ps.setLong(9, schoolId);
            ps.executeUpdate();
        }
    }

    /**
     * 内部强制结束会话
     *
     * @param sessionId 纯 UUID
     */
    private void endSessionInternal(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        String hashKey = SESSION_HASH_KEY_PREFIX + sessionId;
        Map<Object, Object> sessionMap = redisTemplate.opsForHash().entries(hashKey);

        if (sessionMap == null || sessionMap.isEmpty()) {
            return;
        }

        try {
            // 从 Redis map 构建 StudySession 对象
            StudySession session = new StudySession();
            session.setUserId(getInteger(sessionMap, "userId"));
            session.setNodeId(getInteger(sessionMap, "nodeId"));
            session.setCourseId(getInteger(sessionMap, "courseId"));
            session.setSchoolId(getInteger(sessionMap, "schoolId"));
            session.setStartTime(getLongOrDefault(sessionMap, "startTime", System.currentTimeMillis() / 1000));
            session.setLastActive(getLongOrDefault(sessionMap, "lastActive", System.currentTimeMillis() / 1000));
            session.setStartProgress(getString(sessionMap, "startProgress"));
            session.setCurrentProgress(getString(sessionMap, "currentProgress"));
            session.setIp(getString(sessionMap, "ip"));
            session.setTerminal(getString(sessionMap, "terminal"));

            String finalProgress = getString(sessionMap, "currentProgress");
            Result result = saveStudyRecordFromSession(session, finalProgress);

            // 清理 Redis
            cleanupRedis(sessionId, hashKey);

        } catch (Exception e) {
            cleanupRedis(sessionId, hashKey);
        }
    }

    private void cleanupRedis(String sessionId, String hashKey) {
        try {
            redisTemplate.delete(hashKey);
            redisTemplate.opsForZSet().remove(SESSION_ZSET_KEY, sessionId);
        } catch (Exception e) {
            log.warn("清理 Redis 会话 {} 时出错", sessionId, e);
        }
    }

    private static String getString(Map<Object, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : null;
    }

    private static Integer getInteger(Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Long getLong(Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String getStringFromHash(String hashKey, String field, String defaultValue) {
        Object val = redisTemplate.opsForHash().get(hashKey, field);
        return val != null ? val.toString() : defaultValue;
    }

    private long getLongFromHash(String hashKey, String field, long defaultValue) {
        Object val = redisTemplate.opsForHash().get(hashKey, field);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long getLongOrDefault(Map<Object, Object> map, String key, long defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 解析进度值
     */
    private double parseProgress(String p) {
        if (p == null || p.trim().isEmpty()) return 0.0;
        try {
            double v = Double.parseDouble(p.trim());
            return Math.max(0.0, v);
        } catch (Exception e) {
            return 0.0;
        }
    }


    private void upsertCourseStudent(Connection conn, int studentId, int courseId, int schoolId,
                                     int videoLearnedDelta, long studyTimeToAdd) throws SQLException {
        // 加锁查询选课记录
        String lockSql = """
    SELECT videoLearned, videoCount 
    FROM yee_course_student 
    WHERE studentId = ? AND courseId = ? AND schoolId = ? 
    FOR UPDATE
    """;
        boolean exists = false;
        try (PreparedStatement lockPs = conn.prepareStatement(lockSql)) {
            lockPs.setInt(1, studentId);
            lockPs.setInt(2, courseId);
            lockPs.setInt(3, schoolId);
            try (ResultSet rs = lockPs.executeQuery()) {
                if (rs.next()) {
                    exists = true;
                }
            }
        }
        if (!exists) {
            log.error("严重异常：已开启学习会话但无合法选课记录 student:{},course:{},school:{}", studentId, courseId, schoolId);
            return;
        }

        // 仅执行更新逻辑
        String updateSql = """
    UPDATE yee_course_student
    SET
        videoLearned = LEAST(videoLearned + ?, videoCount),
        studyTime = studyTime + ?,
        `change` = `change` + 1
    WHERE studentId = ? AND courseId = ? AND schoolId = ?
    """;
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, videoLearnedDelta);
            ps.setLong(2, studyTimeToAdd);
            ps.setInt(3, studentId);
            ps.setInt(4, courseId);
            ps.setInt(5, schoolId);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) {
                log.error("更新选课汇总无匹配行 student:{},course:{},school:{}", studentId, courseId, schoolId);
            }
        }
    }


    /**
     * 前端百分比 → 数据库小数（核心转换）
     * @param frontProgress 前端传入的 0-100 百分比（如 50 → 0.50，100 → 1.00）
     * @return 保留2位小数的字符串（符合数据库 decimal(18,2) 格式）
     */
    private String front2DbProgress(double frontProgress) {
        // 1. 限定范围 0-100，防止异常值
        double clamped = Math.max(0.0, Math.min(100.0, frontProgress));
        // 2. 转换为 0-1 小数，保留2位（四舍五入）
        BigDecimal dbProgress = BigDecimal.valueOf(clamped)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return dbProgress.toPlainString();
    }

    /**
     * 数据库小数 → 前端百分比（核心转换）
     * @param dbProgress 数据库存储的 0-1.00 小数（如 "0.50" → 50，"1.00" → 100）
     * @return 整数百分比（0-100）
     */
    private int db2FrontProgress(String dbProgress) {
        if (StringUtils.isBlank(dbProgress)) {
            return 0;
        }
        try {
            BigDecimal decimal = new BigDecimal(dbProgress);
            // 转换为百分比并取整（0.50 → 50，1.00 → 100）
            return decimal.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}