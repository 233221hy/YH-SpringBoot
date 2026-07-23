package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.VideoRank;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.service.cache.CacheService;
import cn.xfywz.guozespring.service.teacher.VideoRankService;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 视频排行服务实现
 */
@Slf4j
@Service
public class VideoRankServiceImpl implements VideoRankService {

    @Autowired
    private DatabaseUtil databaseUtil;
    @Autowired
    private CacheService  cacheService;

    private static final long CACHE_TTL_MINUTES = 30; // 排行数据缓存 30 分钟

    @Override
    public Result list(VideoRank param) {
        // 1. 必要参数校验
        if (param == null || param.getSchoolId() == null || param.getCourseId() <= 0) {
            return Result.error("参数不完整");
        }

        int pageNum = param.getPageNum() == null ? 1 : param.getPageNum();
        int pageSize = param.getPageSize() == null ? 10 : param.getPageSize();

        // 2. 构建缓存 Key
        String cacheKey = buildCacheKey(param, pageNum, pageSize);

        // 3. 使用编程式缓存
        return cacheService.getOrLoad(
                cacheKey,
                () -> doList(param, pageNum, pageSize),
                CACHE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    /**
     * 实际查询逻辑
     */
    private Result doList(VideoRank param, int pageNum, int pageSize) {
        int schoolId = param.getSchoolId();

        // 4. 计算总播放时长（用于占比）
        long totalSeconds = queryTotalSeconds(schoolId, param);
        if (totalSeconds <= 0) {
            // 无任何播放记录时返回空列表，避免后续除零
            return Result.success(PageResult.empty());
        }

        // 5. 分页查询排行数据
        PageResult<Map<String, Object>> pageResult = queryRankPage(schoolId, param, pageNum, pageSize, totalSeconds);

        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

    // ==================== 查询方法 ====================

    private long queryTotalSeconds(int schoolId, VideoRank param) {
        QueryBuilder qb = databaseUtil.query(schoolId)
                .sql("SELECT COALESCE(SUM(yst.duration), 0) FROM yee_study_time yst " +
                        "INNER JOIN yee_node yn ON yn.id = yst.nodeId " +
                        "LEFT JOIN yee_student st ON st.id = yst.userId");
        applyCommonConditions(qb, param);
        return qb.scalar(rs -> rs.getLong(1)).orElse(0L);
    }

    private PageResult<Map<String, Object>> queryRankPage(int schoolId, VideoRank param,
                                                          int pageNum, int pageSize, long totalSeconds) {
        String selectCols = """
        SELECT yn.id AS nodeId,
               yn.name AS nodeName,
               COALESCE(yn.videoDuration, 0) AS videoDuration,
               SUM(yst.duration) AS playSeconds,
               COUNT(*) AS playCount
        """;
        String fromSql = " FROM yee_study_time yst " +
                "INNER JOIN yee_node yn ON yn.id = yst.nodeId " +
                "LEFT JOIN yee_student st ON st.id = yst.userId";

        QueryBuilder dataBuilder = databaseUtil.query(schoolId)
                .sql(selectCols + fromSql);
        applyCommonConditions(dataBuilder, param);
        dataBuilder.groupBy("yn.id, yn.name, yn.videoDuration")
                .orderBy("playSeconds DESC, playCount DESC");

        // 1. 使用子查询包装版计数（传入内层别名）
        BuiltSql countSql = dataBuilder.buildCountDistinct("nodeId");
        long total = databaseUtil.executeQuery(schoolId, countSql, rs -> {
            try {
                return rs.next() ? rs.getLong(1) : 0L;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        if (total == 0) return PageResult.empty();

        // 2. 数据分页
        dataBuilder.limit(pageSize).offset((pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = dataBuilder.list(rs -> {
            try {
                return mapRow(rs, totalSeconds);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return PageResult.of(total, rows);
    }

    /**
     * 添加公共查询条件
     */
    private void applyCommonConditions(QueryBuilder qb, VideoRank param) {
        qb.eq("yst.courseId", param.getCourseId())
                .where("yn.id IS NOT NULL");

        if (param.getClassId() != null && param.getClassId() > 0) {
            qb.eq("st.classId", param.getClassId());
        }
        if (param.getStartTime() != null) {
            qb.where("yst.addTime >= FROM_UNIXTIME(?/1000)", param.getStartTime());
        }
        if (param.getEndTime() != null) {
            qb.where("yst.addTime < FROM_UNIXTIME(?/1000)", param.getEndTime());
        }
        if (param.getTerminalType() != null && param.getTerminalType() != 0) {
            if (param.getTerminalType() == 1) { // PC
                qb.where("(yst.terminal LIKE 'pc%')");
            } else if (param.getTerminalType() == 2) { // 移动端
                qb.where("(yst.terminal NOT LIKE 'pc%' OR yst.terminal IS NULL OR yst.terminal = '')");
            }
        }
    }

    /**
     * 结果集行映射
     */
    private Map<String, Object> mapRow(ResultSet rs, long totalSeconds) throws SQLException {
        long playSeconds = rs.getLong("playSeconds");
        double percent = totalSeconds > 0 ? (playSeconds * 100.0 / totalSeconds) : 0.0;

        Map<String, Object> row = new HashMap<>();
        row.put("nodeId", rs.getLong("nodeId"));
        row.put("nodeName", rs.getString("nodeName"));
        row.put("videoDuration", rs.getLong("videoDuration"));
        row.put("playSeconds", playSeconds);
        row.put("playCount", rs.getLong("playCount"));
        row.put("percent", Math.round(percent * 100.0) / 100.0);
        return row;
    }

    // ==================== 缓存键构建 ====================

    private String buildCacheKey(VideoRank param, int pageNum, int pageSize) {
        return String.format("video_rank:%d:%d:%d:%d:%d:%d:%d:%d",
                param.getSchoolId(),
                param.getCourseId(),
                param.getClassId() == null ? 0 : param.getClassId(),
                param.getStartTime() == null ? 0 : param.getStartTime(),
                param.getEndTime() == null ? 0 : param.getEndTime(),
                param.getTerminalType() == null ? 0 : param.getTerminalType(),
                pageNum,
                pageSize);
    }


    /**
     * 构建基础 FROM 子句
     */
    private String buildFromSql() {
        return """
            FROM yee_study_time yst
            INNER JOIN yee_node yn ON yn.id = yst.nodeId
            LEFT JOIN yee_student st ON st.id = yst.userId
            """;
    }
}

//    @Override
//    public Result list(VideoRank param) {
//        try {
//            // 1. 参数校验与默认值
//            if (param == null || param.getSchoolId() == null || param.getCourseId() <= 0) {
//                return Result.error("参数不完整");
//            }
//            int pageNum = (param.getPageNum() == null || param.getPageNum() < 1) ? 1 : param.getPageNum();
//            int pageSize = (param.getPageSize() == null || param.getPageSize() < 1) ? 10 : param.getPageSize();
//
//            String fromSql = buildFromSql();
//
//            // 3. 构建计数查询（COUNT DISTINCT）
//            QueryBuilder countBuilder = databaseUtil.query(param.getSchoolId())
//                    .sql("SELECT 1 " + fromSql);
//            applyRankConditions(countBuilder, param);
//            BuiltSql countSql = countBuilder.buildCountDistinct("yn.id");
//
//            // 4. 构建总时长查询（用于占比计算）
//            QueryBuilder totalBuilder = databaseUtil.query(param.getSchoolId())
//                    .sql("SELECT COALESCE(SUM(yst.duration), 0) " + fromSql);
//            applyRankConditions(totalBuilder, param);
//            BuiltSql totalBuilt = totalBuilder.build();
//
//            // 5. 构建数据查询（SELECT、GROUP BY、ORDER BY、分页）
//            String selectCols = """
//                SELECT yn.id AS nodeId,
//                       yn.name AS nodeName,
//                       COALESCE(yn.videoDuration, 0) AS videoDuration,
//                       SUM(yst.duration) AS playSeconds,
//                       COUNT(*) AS playCount
//                """;
//            QueryBuilder dataBuilder = databaseUtil.query(param.getSchoolId())
//                    .sql(selectCols + fromSql);
//            applyRankConditions(dataBuilder, param);
//            dataBuilder.groupBy("yn.id, yn.name, yn.videoDuration")
//                    .orderBy("playSeconds DESC, playCount DESC")
//                    .limit(pageSize)
//                    .offset((pageNum - 1) * pageSize);
//            BuiltSql dataSql = dataBuilder.build();
//
//            // 6. 执行查询
//            try {
//                // 查询总时长
//                long totalSeconds = databaseUtil.executeQuery(param.getSchoolId(), totalBuilt, rs -> {
//                    try {
//                        return rs.next() ? rs.getLong(1) : 0L;
//                    } catch (SQLException e) {
//                        throw new DatabaseException("查询总时长失败", e);
//                    }
//                });
//
//                // 分页查询数据
//                PageResult<Map<String, Object>> pageResult = databaseUtil.queryPage(
//                        param.getSchoolId(),
//                        dataSql,
//                        countSql,
//                        rs -> {
//                            try {
//                                Map<String, Object> row = new HashMap<>();
//                                long playSeconds = rs.getLong("playSeconds");
//                                row.put("nodeId", rs.getLong("nodeId"));
//                                row.put("nodeName", rs.getString("nodeName"));
//                                row.put("videoDuration", rs.getLong("videoDuration"));
//                                row.put("playSeconds", playSeconds);
//                                row.put("playCount", rs.getLong("playCount"));
//                                // 计算占比（保留两位小数）
//                                double percent = totalSeconds > 0 ? (playSeconds * 100.0 / totalSeconds) : 0.0;
//                                row.put("percent", Math.round(percent * 100.0) / 100.0);
//                                return row;
//                            } catch (SQLException e) {
//                                throw new DatabaseException("结果集映射失败", e);
//                            }
//                        }
//                );
//
//                return Result.success(pageResult.getRows(), pageResult.getTotal());
//
//            } catch (DatabaseException e) {
//                log.error("数据库查询失败", e);
//                throw e;
//            }
//
//        } catch (Exception e) {
//            log.error("视频排行查询异常", e);
//            return Result.error("查询失败：" + e.getMessage());
//        }
//    }
