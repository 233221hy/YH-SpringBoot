package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.entity.mhmain.SlTplNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTplChapterNode {
    long id;
    String name;
    long sort;
    List<SlTplNode> slTplNodes;
}
