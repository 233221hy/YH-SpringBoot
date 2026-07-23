package cn.xfywz.guozespring.entity.mhmain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenChapterNode {
    String name;
    List<SlOpenNode> slOpenNodes;

}
