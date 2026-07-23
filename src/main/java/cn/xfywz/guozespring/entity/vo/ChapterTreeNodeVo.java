package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChapterTreeNodeVo {

    private Long id;
    private String name;
    private Long sort;

    // 子节点：可以是 node 或 exam
    private List<CourseTreeNodeVo> children;
}
