package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.vo.GlobalStatsVO;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.GlobalStatsService;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GlobalStatsServiceImplement implements GlobalStatsService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;
    @Autowired
    private StringRedisTemplate redisTemplate;  // 注入 Redis 模板

    // 缓存前缀 + 学校ID
    private static final String CACHE_KEY_PREFIX = "global:stats:school:";
    // 缓存过期时间（秒），例如 24 小时
    private static final long CACHE_EXPIRE_SECONDS = 3600 * 24;

    /**
     * 使用 UNION ALL 一次获取多个统计指标的 SQL
     */
    private static final String SCHOOL_STATS_SQL = """
        SELECT 'teachingClassCount' AS metric, COUNT(*) AS value FROM yee_classes
        UNION ALL
        SELECT 'questionCount', COUNT(*) FROM yee_question
        UNION ALL
        SELECT 'courseCount', COUNT(*) FROM yee_course
        UNION ALL
        SELECT 'teacherCount', COUNT(*) FROM yee_manage
        UNION ALL
        SELECT 'studentCount', COUNT(*) FROM yee_student
        UNION ALL
        SELECT 'examCount', COUNT(*) FROM yee_exam
        UNION ALL
        SELECT 'workCount', COUNT(*) FROM yee_work
        UNION ALL
        SELECT 'examRecordCount', COUNT(*) FROM yee_exam_record
        UNION ALL
        SELECT 'courseSelectCount', COUNT(*) FROM yee_course_student
        """;

    @Override
    public GlobalStatsVO getGlobalStats() {
        // 1. 获取所有已审核学校
        List<SlSchool> schools = slSchoolMapper.selectList(
                new LambdaQueryWrapper<SlSchool>()
                        .eq(SlSchool::getAllow, 1)
        );

        long teachingClassTotal = 0;
        long questionTotal = 0;
        long courseTotal = 0;
        long teacherTotal = 0;
        long studentTotal = 0;
        long examTotal = 0;
        long workTotal = 0;
        long examRecordTotal = 0;
        // 存储每个学校的选课人数
        Map<String, Long> schoolCourseSelectCountMap = new HashMap<>();

        // 2. 遍历学校，累加各项指标
        for (SlSchool school : schools) {
            try {
                // 2.1 尝试从缓存获取该校统计
                Map<String, Long> schoolStats = getSchoolStatsFromCache(school.getId());
                if (schoolStats == null) {
                    // 缓存未命中，执行查询
                    schoolStats = querySchoolStats(school.getId());
                    // 存入缓存
                    cacheSchoolStats(school.getId(), schoolStats);
                }

                // 2.2 累加全局数据
                teachingClassTotal += schoolStats.getOrDefault("teachingClassCount", 0L);
                questionTotal += schoolStats.getOrDefault("questionCount", 0L);
                courseTotal += schoolStats.getOrDefault("courseCount", 0L);
                teacherTotal += schoolStats.getOrDefault("teacherCount", 0L);
                studentTotal += schoolStats.getOrDefault("studentCount", 0L);
                examTotal += schoolStats.getOrDefault("examCount", 0L);
                workTotal += schoolStats.getOrDefault("workCount", 0L);
                examRecordTotal += schoolStats.getOrDefault("examRecordCount", 0L);
                // 记录该校选课人数
                schoolCourseSelectCountMap.put(school.getName(),
                        schoolStats.getOrDefault("courseSelectCount", 0L));

            } catch (Exception e) {
                log.error("统计学校 {} 数据失败，跳过该学校", school.getId(), e);
            }
        }

        // 3. 构建返回 VO
        GlobalStatsVO vo = new GlobalStatsVO();
        vo.setTeachingClassCount(teachingClassTotal);
        vo.setQuestionCount(questionTotal);
        vo.setCourseCount(courseTotal);
        vo.setTeacherCount(teacherTotal);
        vo.setStudentCount(studentTotal);
        vo.setAssessmentCount(examTotal + workTotal);
        vo.setExamPersonCount(examRecordTotal);
        vo.setSchoolCourseSelectCountMap(schoolCourseSelectCountMap);
        return vo;
    }

    /**
     * 从 Redis 缓存中获取某学校的统计数据
     * @param schoolId 学校ID
     * @return Map(指标名 -> 数值) 或 null（缓存未命中或异常）
     */
    private Map<String, Long> getSchoolStatsFromCache(Integer schoolId) {
        String key = CACHE_KEY_PREFIX + schoolId;
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) {
                return null;
            }
            Map<String, Long> stats = new HashMap<>();
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String metric = (String) entry.getKey();
                String valueStr = (String) entry.getValue();
                stats.put(metric, Long.parseLong(valueStr));
            }
            return stats;
        } catch (Exception e) {
            log.warn("读取 Redis 缓存失败, key={}", key, e);
            return null; // 缓存读取失败时回退到查询数据库
        }
    }

    /**
     * 将某学校的统计数据存入 Redis 缓存
     * @param schoolId 学校ID
     * @param stats Map(指标名 -> 数值)
     */
    private void cacheSchoolStats(Integer schoolId, Map<String, Long> stats) {
        String key = CACHE_KEY_PREFIX + schoolId;
        try {
            Map<String, String> stringStats = new HashMap<>();
            stats.forEach((k, v) -> stringStats.put(k, String.valueOf(v)));
            redisTemplate.opsForHash().putAll(key, stringStats);
            redisTemplate.expire(key, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入 Redis 缓存失败, key={}", key, e);
        }
    }

    /**
     * 直接查询某学校的统计数据（不使用缓存）
     * @param schoolId 学校ID
     * @return Map(指标名 -> 数值)
     */
    private Map<String, Long> querySchoolStats(Integer schoolId) {
        return databaseUtil.executeQuery(
                schoolId,
                SCHOOL_STATS_SQL,
                rs -> {
                    Map<String, Long> map = new HashMap<>();
                    try {
                        while (rs.next()) {
                            String metric = rs.getString("metric");
                            Long value = rs.getLong("value");
                            map.put(metric, value);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException("解析统计数据失败", e);
                    }
                    return map;
                }
        );
    }
}