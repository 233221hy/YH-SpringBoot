package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@TableName(value = "yee_discuss_reply")
@Data
public class YeeDiscussReply {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer discussId;
    private Integer courseId;
    private Integer userId;
    private String content;
    private Date addTime;

    // ✅ images 保持 List<String>（前端仍传字符串数组）
    private List<String> images;

    // ❌ 不再使用 files 字段（废弃）
    // private List<String> files;

    // ✅ 新增：attachFiles 用于带文件名的附件
    private List<FileInfo> attachFiles;

    // 数据库存储字段（JSON 字符串）
    @TableField("files")
    private String filesJson; // 对应 attachFiles 的 JSON

    private Integer pid;
    private Integer reUserId;
    private Integer classId;
    private Integer replyId;
    private Integer isDelete;
    private String platform;
    private Integer schoolId;
    private Date addDate;

    // Getter/Setter for attachFiles → filesJson
    public List<FileInfo> getAttachFiles() {
        if (this.attachFiles != null) return this.attachFiles;
        if (this.filesJson == null || this.filesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<List<FileInfo>>(){}.getType();
            return new Gson().fromJson(this.filesJson, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setAttachFiles(List<FileInfo> attachFiles) {
        this.attachFiles = attachFiles;
        if (attachFiles == null || attachFiles.isEmpty()) {
            this.filesJson = null;
        } else {
            this.filesJson = new Gson().toJson(attachFiles);
        }
    }
}