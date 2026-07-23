package cn.xfywz.guozespring.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeDiscussVo {
    private long id;
    private String title;
    private long teacherId;
    private String teacherName;
    private String teacherAvatar;
    private Timestamp addTime;
    private String content;
    private String images;
    private long classId;
    private long courseId;
    private long top;
    private String files;
    private long isDelete;
    private long changeTime;
    private long schoolId;
    private Date addDate;

    private Integer participantCount; // 参与人数
    private Integer replyCount;       // 评论数
    private String courseName;        // 课程名称
    private  String userName;         // 用户姓名
    private  String userAvatar;       // 用户头像


}
