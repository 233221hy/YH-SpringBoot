package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.constant.RedisKeyConstants;
import cn.xfywz.guozespring.service.teacher.OnlineUserStatisticsService;
import cn.xfywz.guozespring.util.RedisUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserStatisticsServiceImpl implements OnlineUserStatisticsService {

    private final RedisUtils redisUtils;

    // ================== 10分钟不活跃 = 离线 ==================
    private static final long ONLINE_EXPIRE_MS = 10 * 60 * 1000;

    /**
     * 【标准】按小时统计（今天活跃）
     */
    @Override
    public Map<String, Map<String, Integer>> statisticsTodayOnlineUserByHour() {
        Map<String, Map<String, Integer>> result = initHourResult();
        long[] timeRange = getTodayTimeRange();
        long start = timeRange[0];
        long end = timeRange[1];

        statisticsTodayActiveForAllSchool(RedisKeyConstants.USER_TYPE_STUDENT, start, end, result);
        statisticsTodayActiveForAllSchool(RedisKeyConstants.USER_TYPE_MANAGE, start, end, result);
        calculateHourTotal(result);
        return result;
    }

    /**
     * 【折线图专用】过去24小时 - 20分钟分段（当前时间往前推24h）
     */
    @Override
    public Map<String, Map<String, Integer>> statisticsTodayOnlineUserBy20Min() {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        long past24h = now - 24 * 60 * 60 * 1000L;

        List<String> timeKeys = generate20MinTimeKeys(past24h, now);
        for (String key : timeKeys) {
            Map<String, Integer> data = new HashMap<>();
            data.put(RedisKeyConstants.USER_TYPE_STUDENT, 0);
            data.put(RedisKeyConstants.USER_TYPE_MANAGE, 0);
            data.put("total", 0);
            result.put(key, data);
        }

        countActiveTo20MinRange(past24h, now, result);
        calculate20MinTotal(result);
        return result;
    }

    /**
     * 【学校折线图专用】过去24小时 - 按小时
     */
    @Override
    public Map<String, Map<String, Integer>> statisticsTodayOnlineBySchool(Long schoolId) {
        if (schoolId == null || schoolId <= 0) {
            throw new IllegalArgumentException("学校ID不能为空且必须大于0");
        }

        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        long past24h = now - 24 * 60 * 60 * 1000L;

        List<String> hourKeys = generateHourTimeKeys(past24h, now);
        for (String key : hourKeys) {
            Map<String, Integer> data = new HashMap<>();
            data.put(RedisKeyConstants.USER_TYPE_STUDENT, 0);
            data.put(RedisKeyConstants.USER_TYPE_MANAGE, 0);
            data.put("total", 0);
            result.put(key, data);
        }

        countSchoolActiveToHourRange(schoolId, past24h, now, result);
        calculateHourTotal(result);
        return result;
    }

    /**
     * 【行业标准】真实当前在线人数（10分钟内活跃）
     */
    @Override
    public Map<String, Integer> getCurrentOnlineTotal() {
        Map<String, Integer> result = new HashMap<>();
        long now = System.currentTimeMillis();
        long validTime = now - ONLINE_EXPIRE_MS;

        int student = countRealOnlineUser(RedisKeyConstants.USER_TYPE_STUDENT, validTime);
        int manage = countRealOnlineUser(RedisKeyConstants.USER_TYPE_MANAGE, validTime);

        result.put("student", student);
        result.put("manage", manage);
        result.put("total", student + manage);
        return result;
    }

    public Map<String, Integer> getCurrentOnlineBySchool(Long schoolId) {
        Map<String, Integer> result = new HashMap<>();
        long now = System.currentTimeMillis();
        long validTime = now - ONLINE_EXPIRE_MS;

        int student = countRealOnlineUserBySchool(RedisKeyConstants.USER_TYPE_STUDENT, schoolId, validTime);
        int manage = countRealOnlineUserBySchool(RedisKeyConstants.USER_TYPE_MANAGE, schoolId, validTime);

        result.put("student", student);
        result.put("manage", manage);
        result.put("total", student + manage);
        return result;
    }

    // -------------------------------------------------------------------------
    // 私有工具：今日活跃统计
    // -------------------------------------------------------------------------
    private void statisticsTodayActiveForAllSchool(String userType, long start, long end, Map<String, Map<String, Integer>> result) {
        try {
            Set<String> keys = redisUtils.keys("online:" + userType + ":*:*");
            if (CollectionUtils.isEmpty(keys)) return;

            Set<String> countedUsers = new HashSet<>();
            for (String key : keys) {
                Object timeObj = redisUtils.get(key);
                if (timeObj == null) continue;

                try {
                    long ts = Long.parseLong(timeObj.toString());
                    if (ts >= start && ts <= end && !countedUsers.contains(key)) {
                        String hour = String.format("%02d", LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).getHour());
                        result.get(hour).put(userType, result.get(hour).get(userType) + 1);
                        countedUsers.add(key);
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            log.error("statisticsTodayActiveForAllSchool error", e);
        }
    }

    private void statisticsTodayActiveBySchool(String userType, Long schoolId, long start, long end, Map<String, Map<String, Integer>> result) {
        try {
            String pattern = RedisKeyConstants.buildOnlineUserPattern(userType, schoolId);
            Set<String> keys = redisUtils.keys(pattern);
            if (CollectionUtils.isEmpty(keys)) return;

            Set<String> countedUsers = new HashSet<>();
            for (String key : keys) {
                Object timeObj = redisUtils.get(key);
                if (timeObj == null) continue;

                try {
                    long ts = Long.parseLong(timeObj.toString());
                    if (ts >= start && ts <= end && !countedUsers.contains(key)) {
                        String hour = String.format("%02d", LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).getHour());
                        result.get(hour).put(userType, result.get(hour).get(userType) + 1);
                        countedUsers.add(key);
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            log.error("statisticsTodayActiveBySchool error", e);
        }
    }

    // 真实在线人数统计
    private int countRealOnlineUser(String userType, long validTime) {
        int count = 0;
        Set<String> keys = redisUtils.keys("online:" + userType + ":*:*");
        if (CollectionUtils.isEmpty(keys)) return 0;

        for (String key : keys) {
            Object timeObj = redisUtils.get(key);
            if (timeObj == null) continue;
            try {
                long ts = Long.parseLong(timeObj.toString());
                if (ts >= validTime) count++;
            } catch (Exception ignore) {}
        }
        return count;
    }

    private int countRealOnlineUserBySchool(String userType, Long schoolId, long validTime) {
        int count = 0;
        String pattern = RedisKeyConstants.buildOnlineUserPattern(userType, schoolId);
        Set<String> keys = redisUtils.keys(pattern);
        if (CollectionUtils.isEmpty(keys)) return 0;

        for (String key : keys) {
            Object timeObj = redisUtils.get(key);
            if (timeObj == null) continue;
            try {
                long ts = Long.parseLong(timeObj.toString());
                if (ts >= validTime) count++;
            } catch (Exception ignore) {}
        }
        return count;
    }

    // 图表专用：过去24小时 20分钟分段统计
    private List<String> generate20MinTimeKeys(long startMs, long endMs) {
        List<String> keys = new ArrayList<>();
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault());
        int alignMin = (time.getMinute() / 20) * 20;
        time = time.withMinute(alignMin).withSecond(0).withNano(0);

        while (true) {
            long currentMs = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (currentMs > endMs) break;

            String start = String.format("%02d:%02d", time.getHour(), time.getMinute());
            LocalDateTime endTime = time.plusMinutes(20);
            String end = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute() % 60);
            keys.add(start + "-" + end);

            time = time.plusMinutes(20);
        }
        return keys;
    }

    private void countActiveTo20MinRange(long start, long end, Map<String, Map<String, Integer>> result) {
        Set<String> counted = new HashSet<>();

        // 学生
        Set<String> studentKeys = redisUtils.keys("online:" + RedisKeyConstants.USER_TYPE_STUDENT + ":*:*");
        if (studentKeys != null) {
            for (String key : studentKeys) {
                Object timeObj = redisUtils.get(key);
                if (timeObj == null) continue;
                try {
                    long ts = Long.parseLong(timeObj.toString());
                    if (ts >= start && ts <= end && !counted.contains(key)) {
                        String timeKey = get20MinTimeKey(ts);
                        Map<String, Integer> data = result.get(timeKey);
                        if (data != null) {
                            data.put(RedisKeyConstants.USER_TYPE_STUDENT, data.get(RedisKeyConstants.USER_TYPE_STUDENT) + 1);
                            counted.add(key);
                        }
                    }
                } catch (Exception ignore) {}
            }
        }

        // 管理员
        Set<String> manageKeys = redisUtils.keys("online:" + RedisKeyConstants.USER_TYPE_MANAGE + ":*:*");
        if (manageKeys != null) {
            for (String key : manageKeys) {
                Object timeObj = redisUtils.get(key);
                if (timeObj == null) continue;
                try {
                    long ts = Long.parseLong(timeObj.toString());
                    if (ts >= start && ts <= end && !counted.contains(key)) {
                        String timeKey = get20MinTimeKey(ts);
                        Map<String, Integer> data = result.get(timeKey);
                        if (data != null) {
                            data.put(RedisKeyConstants.USER_TYPE_MANAGE, data.get(RedisKeyConstants.USER_TYPE_MANAGE) + 1);
                            counted.add(key);
                        }
                    }
                } catch (Exception ignore) {}
            }
        }
    }

    private String get20MinTimeKey(long timestamp) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        int groupMin = (time.getMinute() / 20) * 20;
        String start = String.format("%02d:%02d", time.getHour(), groupMin);
        LocalDateTime endTime = time.plusMinutes(20 - time.getMinute() % 20);
        String end = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute() % 60);
        return start + "-" + end;
    }

    // 图表专用：学校 过去24小时 按小时
    private List<String> generateHourTimeKeys(long startMs, long endMs) {
        List<String> keys = new ArrayList<>();
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault())
                .withMinute(0).withSecond(0).withNano(0);

        while (true) {
            long currentMs = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (currentMs > endMs) break;

            keys.add(String.format("%02d", time.getHour()));
            time = time.plusHours(1);
        }
        return keys;
    }

    private void countSchoolActiveToHourRange(Long schoolId, long start, long end, Map<String, Map<String, Integer>> result) {
        Set<String> counted = new HashSet<>();

        // 学生
        String studentPattern = RedisKeyConstants.buildOnlineUserPattern(RedisKeyConstants.USER_TYPE_STUDENT, schoolId);
        Set<String> studentKeys = redisUtils.keys(studentPattern);
        if (studentKeys != null) {
            for (String key : studentKeys) {
                Object timeObj = redisUtils.get(key);
                if (timeObj == null) continue;
                try {
                    long ts = Long.parseLong(timeObj.toString());
                    if (ts >= start && ts <= end && !counted.contains(key)) {
                        String hourKey = String.format("%02d", LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).getHour());
                        Map<String, Integer> data = result.get(hourKey);
                        if (data != null) {
                            data.put(RedisKeyConstants.USER_TYPE_STUDENT, data.get(RedisKeyConstants.USER_TYPE_STUDENT) + 1);
                            counted.add(key);
                        }
                    }
                } catch (Exception ignore) {}
            }
        }

        // 管理员
        String managePattern = RedisKeyConstants.buildOnlineUserPattern(RedisKeyConstants.USER_TYPE_MANAGE, schoolId);
        Set<String> manageKeys = redisUtils.keys(managePattern);
        if (manageKeys != null) {
            for (String key : manageKeys) {
                Object timeObj = redisUtils.get(key);
                if (timeObj == null) continue;
                try {
                    long ts = Long.parseLong(timeObj.toString());
                    if (ts >= start && ts <= end && !counted.contains(key)) {
                        String hourKey = String.format("%02d", LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).getHour());
                        Map<String, Integer> data = result.get(hourKey);
                        if (data != null) {
                            data.put(RedisKeyConstants.USER_TYPE_MANAGE, data.get(RedisKeyConstants.USER_TYPE_MANAGE) + 1);
                            counted.add(key);
                        }
                    }
                } catch (Exception ignore) {}
            }
        }
    }

    // 基础工具
    private Map<String, Map<String, Integer>> initHourResult() {
        Map<String, Map<String, Integer>> map = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Integer> data = new HashMap<>();
            data.put(RedisKeyConstants.USER_TYPE_STUDENT, 0);
            data.put(RedisKeyConstants.USER_TYPE_MANAGE, 0);
            data.put("total", 0);
            map.put(String.format("%02d", i), data);
        }
        return map;
    }

    private long[] getTodayTimeRange() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999);
        return new long[]{
                start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        };
    }

    private void calculateHourTotal(Map<String, Map<String, Integer>> result) {
        result.forEach((k, v) -> v.put("total", v.get(RedisKeyConstants.USER_TYPE_STUDENT) + v.get(RedisKeyConstants.USER_TYPE_MANAGE)));
    }

    private void calculate20MinTotal(Map<String, Map<String, Integer>> result) {
        result.values().forEach(v -> v.put("total", v.get(RedisKeyConstants.USER_TYPE_STUDENT) + v.get(RedisKeyConstants.USER_TYPE_MANAGE)));
    }
}