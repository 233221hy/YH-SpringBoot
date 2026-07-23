package cn.xfywz.guozespring.entity.mhsch;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeDiscuss {
  private long id;
  private String title;
  private long teacherId;
  private java.sql.Timestamp addTime;
  private String content;

  private String images;

  private long classId;
  private long courseId;
  private long top;

  private String files; // 数据库存储用

  private List<FileInfo> attachFiles;

  private long isDelete;
  private long changeTime;
  private long schoolId;
  private java.sql.Date addDate;

  public List<FileInfo> getAttachFiles() {
    if (this.attachFiles != null) return this.attachFiles;
    if (this.files == null || this.files.trim().isEmpty()) {
      return new ArrayList<>();
    }
    try {
      Type listType = new TypeToken<List<FileInfo>>(){}.getType();
      return new Gson().fromJson(this.files, listType);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  public void setAttachFiles(List<FileInfo> attachFiles) {
    this.attachFiles = attachFiles;
    if (attachFiles == null || attachFiles.isEmpty()) {
      this.files = null;
    } else {
      this.files = new Gson().toJson(attachFiles);
    }
  }
}