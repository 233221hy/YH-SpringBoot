package cn.xfywz.guozespring.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseTreeNodeVo {
    private long id;
    private String name;
    private long sort;

    // Tabs
    private long tabVideo;
    private long tabFile;
    private long tabVote;
    private long tabWork;
    private long tabExam;

    // 节点详细信息（新增）
    private String type;
    private String videoFile;
    private long videoDuration;
    private String votingPath;
    private long videoMode;
    private String localFile;
    private long lock;
    private long unlockTime;
}
