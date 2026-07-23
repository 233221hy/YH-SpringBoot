package cn.xfywz.guozespring.entity.mhmain;


import com.baomidou.mybatisplus.annotation.TableField;

public class SlTplCourse {

  private long id;
  private String name;
  private long mode;
  private String code;
  @TableField(value = "categoryId")
  private String categoryId;
  @TableField(value = "cateBid")
  private long cateBid;
  @TableField(value = "cateMid")
  private long cateMid;
  private double credit;
  private String cover;
  private String intro;
  private String content;
  @TableField(value = "teacherIntro")
  private String teacherIntro;
  private long allow;
  @TableField(value = "addTime")
  private java.sql.Timestamp addTime;
  @TableField(value = "createId")
  private long createId;
  private long weight;
  @TableField(value = "schoolId")
  private long schoolId;
  private long quote;
  private String teacher;
  private String school;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  public long getMode() {
    return mode;
  }

  public void setMode(long mode) {
    this.mode = mode;
  }


  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }


  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }


  public long getCateBid() {
    return cateBid;
  }

  public void setCateBid(long cateBid) {
    this.cateBid = cateBid;
  }


  public long getCateMid() {
    return cateMid;
  }

  public void setCateMid(long cateMid) {
    this.cateMid = cateMid;
  }


  public double getCredit() {
    return credit;
  }

  public void setCredit(double credit) {
    this.credit = credit;
  }


  public String getCover() {
    return cover;
  }

  public void setCover(String cover) {
    this.cover = cover;
  }


  public String getIntro() {
    return intro;
  }

  public void setIntro(String intro) {
    this.intro = intro;
  }


  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }


  public String getTeacherIntro() {
    return teacherIntro;
  }

  public void setTeacherIntro(String teacherIntro) {
    this.teacherIntro = teacherIntro;
  }


  public long getAllow() {
    return allow;
  }

  public void setAllow(long allow) {
    this.allow = allow;
  }


  public java.sql.Timestamp getAddTime() {
    return addTime;
  }

  public void setAddTime(java.sql.Timestamp addTime) {
    this.addTime = addTime;
  }


  public long getCreateId() {
    return createId;
  }

  public void setCreateId(long createId) {
    this.createId = createId;
  }


  public long getWeight() {
    return weight;
  }

  public void setWeight(long weight) {
    this.weight = weight;
  }


  public long getSchoolId() {
    return schoolId;
  }

  public void setSchoolId(long schoolId) {
    this.schoolId = schoolId;
  }


  public long getQuote() {
    return quote;
  }

  public void setQuote(long quote) {
    this.quote = quote;
  }


  public String getTeacher() {
    return teacher;
  }

  public void setTeacher(String teacher) {
    this.teacher = teacher;
  }


  public String getSchool() {
    return school;
  }

  public void setSchool(String school) {
    this.school = school;
  }

}
