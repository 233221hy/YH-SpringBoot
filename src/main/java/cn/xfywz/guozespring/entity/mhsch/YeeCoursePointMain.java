package cn.xfywz.guozespring.entity.mhsch;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Builder
public class YeeCoursePointMain implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer courseId;

    private Integer studentId;

    private Integer point;

    private Integer rank;

    private Integer point2;
}