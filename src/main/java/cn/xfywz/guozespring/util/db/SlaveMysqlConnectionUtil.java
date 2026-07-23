package cn.xfywz.guozespring.util.db;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class SlaveMysqlConnectionUtil {

    // ==================== 核心常量 ====================
    public static final long MAX_IDLE_MS = 1000L * 60 * 30; // 30分钟空闲销毁
    private static final long CLEAN_INTERVAL_MS = 1000L * 60; // 1分钟清理一次

    // ==================== 存储结构 ====================
    private static final ConcurrentHashMap<Integer, HikariDataSource> DATA_SOURCE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Long> LAST_USED_TIME = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> CURRENT_SCHOOL_ID = new ThreadLocal<>();
    private static final ReentrantLock CLOSE_LOCK = new ReentrantLock();

    static {
        startIdleDataSourceCleaner();
        registerJvmShutdownHook();
    }

    // ==================== 获取连接（核心） ====================
    public static Connection getConnection(SlSchool slSchool) throws SQLException {
        if (slSchool == null || slSchool.getId() == null) {
            throw new IllegalArgumentException("SlSchool or schoolId is null");
        }
        Integer schoolId = slSchool.getId();

        // 关键：切换学校时强制清理，彻底杜绝串库
        if (!schoolId.equals(CURRENT_SCHOOL_ID.get())) {
            cleanup();
        }

        // 更新最后使用时间（在加锁前，非关键操作）
        LAST_USED_TIME.put(schoolId, System.currentTimeMillis());

        // 获取或创建数据源（加锁双检，防止空闲清理线程并发关闭）
        HikariDataSource dataSource = getOrCreateDataSource(schoolId, slSchool);

        CURRENT_SCHOOL_ID.set(schoolId);
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            // 防御 TOCTOU：isClosed() 检查通过后，清理线程可能恰好关闭了池
            if (isPoolClosedException(e)) {
                log.warn("获取连接时发现数据源已被关闭，重建数据源 schoolId={}", schoolId);
                CLOSE_LOCK.lock();
                try {
                    closeDataSource(schoolId, DATA_SOURCE_CACHE.remove(schoolId));
                    dataSource = createDataSource(slSchool);
                    DATA_SOURCE_CACHE.put(schoolId, dataSource);
                } finally {
                    CLOSE_LOCK.unlock();
                }
                return dataSource.getConnection();
            }
            throw e;
        }
    }

    /**
     * 安全获取或创建数据源，使用 CLOSE_LOCK 与清理线程互斥
     */
    private static HikariDataSource getOrCreateDataSource(int schoolId, SlSchool slSchool) throws SQLException {
        HikariDataSource ds = DATA_SOURCE_CACHE.get(schoolId);
        if (ds != null && !ds.isClosed()) {
            return ds;
        }
        CLOSE_LOCK.lock();
        try {
            // 双重检查：锁内再次确认是否需要创建
            ds = DATA_SOURCE_CACHE.get(schoolId);
            if (ds != null && !ds.isClosed()) {
                return ds;
            }
            // 清理已关闭的旧数据源
            if (ds != null) {
                closeDataSource(schoolId, ds);
            }
            ds = createDataSource(slSchool);
            DATA_SOURCE_CACHE.put(schoolId, ds);
            log.warn("数据源已创建 schoolId={}, db={}", schoolId, slSchool.getDbName());
            return ds;
        } catch (Exception e) {
            log.error("创建数据源失败，学校ID:{}", schoolId, e);
            throw new SQLException("创建从库数据源失败", e);
        } finally {
            CLOSE_LOCK.unlock();
        }
    }

    /** 判断是否属于连接池已关闭类异常（HikariCP 关闭时消息包含 "closed"） */
    private static boolean isPoolClosedException(SQLException e) {
        if (e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase();
        return msg.contains("closed") || msg.contains("shutdown") || msg.contains("pool");
    }

    // 在 SlaveMysqlConnectionUtil 里
    public static void cleanup() {
        try {
            CURRENT_SCHOOL_ID.remove();
        } catch (Exception e) {
            log.warn("清理ThreadLocal异常", e);
        }
//        CURRENT_SCHOOL_ID.set(null);
    }

    // ==================== 创建数据源 ====================
    private static HikariDataSource createDataSource(SlSchool school) {
        if (school.getDbHost() == null || school.getDbPort() == null ||
                school.getDbName() == null || school.getDbUser() == null) {
            throw new IllegalArgumentException("数据库配置不完整，学校ID:" + school.getId());
        }

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%s/%s" +
                        "?useUnicode=true&characterEncoding=utf8" +
                        "&serverTimezone=Asia/Shanghai" +
                        "&rewriteBatchedStatements=true" +
                        "&cachePrepStmts=true&prepStmtCacheSize=500&prepStmtCacheSqlLimit=4096" +
                        "&useServerPrepStmts=true&cacheResultSetMetadata=true" +
                        "&tcpKeepAlive=true&useSSL=false&allowPublicKeyRetrieval=true" +
                        "&autoReconnect=false&allowMultiQueries=false",
                school.getDbHost(), school.getDbPort(), school.getDbName()
        );

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(school.getDbUser());
        config.setPassword(school.getDbPass() == null ? "" : school.getDbPass());

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(3000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(540000);
        config.setLeakDetectionThreshold(300000);
        config.setPoolName("SlaveDS-" + school.getId());

        // ===== 连接有效性检测 =====
        config.setConnectionTestQuery("SELECT 1");   // 借出前用此查询测试
        config.setValidationTimeout(3000);           // 测试超时时间

        // 主动保活，每隔 30 秒发送一次查询，防止服务器断开
        config.setKeepaliveTime(30000);

        return new HikariDataSource(config);
    }

    // ==================== 空闲数据源清理线程 ====================
    private static void startIdleDataSourceCleaner() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CLEAN_INTERVAL_MS);
                    long now = System.currentTimeMillis();

                    Iterator<Map.Entry<Integer, HikariDataSource>> it = DATA_SOURCE_CACHE.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Integer, HikariDataSource> entry = it.next();
                        Integer schoolId = entry.getKey();
                        Long lastUsed = LAST_USED_TIME.get(schoolId);

                        if (lastUsed != null && now - lastUsed > MAX_IDLE_MS) {
                        CLOSE_LOCK.lock();
                        try {
                            log.warn("数据源空闲超时，关闭 schoolId={}", schoolId);
                            closeDataSource(schoolId, entry.getValue());
                            it.remove();
                            LAST_USED_TIME.remove(schoolId);
                        } finally {
                            CLOSE_LOCK.unlock();
                        }
                    }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("数据源清理线程异常", e);
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("slave-ds-cleaner");
        thread.start();
    }

    // ==================== 安全关闭 ====================
    private static void closeDataSource(Integer schoolId, HikariDataSource ds) {
        if (ds == null || ds.isClosed()) return;
        try {
            ds.close();
        } catch (Exception e) {
            log.error("关闭数据源失败，学校ID:{}", schoolId, e);
        }
    }

    // ==================== JVM 关闭钩子 ====================
    private static void registerJvmShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            closeAllDataSources();
        }));
    }

    public static void closeAllDataSources() {
        CLOSE_LOCK.lock();
        try {
            DATA_SOURCE_CACHE.forEach(SlaveMysqlConnectionUtil::closeDataSource);
            DATA_SOURCE_CACHE.clear();
            LAST_USED_TIME.clear();
        } finally {
            CLOSE_LOCK.unlock();
        }
    }

    public static void removeSchoolDataSource(Integer schoolId) {
        if (schoolId == null) return;
        CLOSE_LOCK.lock();
        try {
            HikariDataSource ds = DATA_SOURCE_CACHE.remove(schoolId);
            LAST_USED_TIME.remove(schoolId);
            closeDataSource(schoolId, ds);
        } finally {
            CLOSE_LOCK.unlock();
        }
    }
}