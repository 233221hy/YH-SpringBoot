package cn.xfywz.guozespring.util.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * JDBC参数统一设置工具。
 * 提供将 List<Object> 参数按类型安全地设置到 PreparedStatement 的能力。
 */
public final class JdbcParamsUtil {
    private JdbcParamsUtil() {}

    /**
     * 将参数列表依次设置到 PreparedStatement。
     * 支持 String、Long、Double、Integer、Date 以及其它对象类型的 setObject。
     */
    public static void setParams(PreparedStatement st, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            int idx = i + 1;
            if (param instanceof String) {
                st.setString(idx, (String) param);
            } else if (param instanceof Long) {
                st.setLong(idx, (Long) param);
            } else if (param instanceof Double) {
                st.setDouble(idx, (Double) param);
            } else if (param instanceof Integer) {
                st.setInt(idx, (Integer) param);
            } else if (param instanceof Boolean) {
                st.setBoolean(idx, (Boolean) param);
            } else if (param instanceof Date) {
                st.setDate(idx, (Date) param);
            } else if (param instanceof Timestamp) {
                st.setTimestamp(idx, (Timestamp) param);
            } else if (param instanceof LocalDate) {
                st.setDate(idx, Date.valueOf((LocalDate) param));
            } else if (param instanceof LocalDateTime) {
                st.setTimestamp(idx, Timestamp.valueOf((LocalDateTime) param));
            } else if (param instanceof BigDecimal) {
                st.setBigDecimal(idx, (BigDecimal) param);
            } else st.setObject(idx, param);
        }
    }

    public static void setParams(PreparedStatement st, Object... params) throws SQLException {
        if (params == null || params.length == 0) return;
        setParams(st, java.util.Arrays.asList(params));
    }
}
