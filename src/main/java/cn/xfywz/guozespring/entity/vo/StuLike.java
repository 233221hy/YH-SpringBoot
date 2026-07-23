package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StuLike {
    int schoolId;
    String like;
    String idCard;
    String gender;
    String classId;
    String collegeId;
    String entryYear;
}
