package cn.xfywz.guozespring.entity.dto;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import lombok.Data;

import java.util.List;

@Data
public class TopicWithCoursesDTO {
    private Integer id;
    private String name;
    private String background;
    private List<String> tags;
    private List<SlOpenCourse> courses;
}