package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.entity.mhsch.YeeExamTopic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectedWorkGroup {


    private Long workId;
    private String workTitle;
    private String courseName;
    private String chapterName;
    private Date lastAddTime;
    private Integer topicCount;
    private List<YeeExamTopic> topics;
}
