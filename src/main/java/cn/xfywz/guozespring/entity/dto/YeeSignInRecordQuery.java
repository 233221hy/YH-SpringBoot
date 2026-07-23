package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeSignInRecordQuery {
    private Integer pageNum;
    private Integer pageSize;
    private Integer schoolId;
    private Integer signId;

    //条件查询
    private String keyword;// 学生学号/姓名
    private Integer state;
    private Integer classId;
    private Integer gender;

}
