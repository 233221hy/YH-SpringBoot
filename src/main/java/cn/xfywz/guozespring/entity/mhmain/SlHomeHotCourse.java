package cn.xfywz.guozespring.entity.mhmain;


public class SlHomeHotCourse {

  private long id;
  private long allow;
  private java.sql.Timestamp addTime;
  private long courseId;
  private long sort;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
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


  public long getCourseId() {
    return courseId;
  }

  public void setCourseId(long courseId) {
    this.courseId = courseId;
  }


  public long getSort() {
    return sort;
  }

  public void setSort(long sort) {
    this.sort = sort;
  }

}
