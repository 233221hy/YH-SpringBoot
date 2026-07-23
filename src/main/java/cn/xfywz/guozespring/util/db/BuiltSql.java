package cn.xfywz.guozespring.util.db;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SQL与参数承载体
 * 不包含任何构建逻辑，只用于数据传输
 */
public record BuiltSql(String sql, List<Object> params) {
    public BuiltSql(String sql, List<Object> params) {
        this.sql = sql;
        this.params = params == null ? Collections.emptyList() : List.copyOf(params);
    }

    public static BuiltSql of(String sql, List<Object> params) {
        return new BuiltSql(sql, params);
    }

    public static BuiltSql of(String sql, Object... params) {
        return new BuiltSql(sql, params == null ? Collections.emptyList() : Arrays.asList(params));
    }

}
