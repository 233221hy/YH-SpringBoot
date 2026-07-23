package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenCourseQueryDTO {
    //分页参数
    private Integer pageNum;
    private Integer pageSize;


    //条件参数
    private String name;
    private String cateBid; //学科一级分类
    private String cateMid; //学科二级分类
    private Integer state; //审核状态
    private String code; //课程代码

//    private String schoolAllow; //学校审核
//    private String allow; //教师上架
}
