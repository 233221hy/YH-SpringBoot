package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTplQuestionLike {
    private String like;
    private Integer type;
    private Integer level;
    private Integer cateBid;
    private Integer cateMid;
    private Integer PageSize;
    private Integer PageNum;
}
