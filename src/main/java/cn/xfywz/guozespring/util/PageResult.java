package cn.xfywz.guozespring.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页结果封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResult<T> {
  private long total;
  private List<T> rows;

  /**
   * 静态工厂方法
   */
  public static <T> PageResult<T> of(long total, List<T> rows) {
    return new PageResult<>(total, rows);
  }

  /**
   * 创建空分页结果
   */
  public static <T> PageResult<T> empty() {
    return new PageResult<>(0, new ArrayList<>());
  }

  /**
   * 是否有数据
   */
  public boolean hasRows() {
    return rows != null && !rows.isEmpty();
  }

  /**
   * 获取数据条数
   */
  public int getSize() {
    return rows != null ? rows.size() : 0;
  }
}
