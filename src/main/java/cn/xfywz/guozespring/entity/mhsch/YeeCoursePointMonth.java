package cn.xfywz.guozespring.entity.mhsch;

import lombok.*;

import java.time.LocalDate;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Builder
public class YeeCoursePointMonth implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer courseId;

    private LocalDate month;  // 存储的是年月日中的 "月"，例如 '2025-09-01'

    private Integer studentId;

    private Integer point;

    private Integer rank;

    private Integer point2;
}