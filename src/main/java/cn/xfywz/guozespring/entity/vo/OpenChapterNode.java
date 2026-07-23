package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.entity.mhmain.SlOpenChapter;
import cn.xfywz.guozespring.entity.mhmain.SlOpenNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class OpenChapterNode extends SlOpenChapter {
    private List<SlOpenNode> slOpenNodes;
}
