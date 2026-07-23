package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlSchool {
  private Integer id;
  private String name;
  private String nameEn;
  private String ident;
  private String area;
  private Long province;
  private Long city;
  private Long region;
  private String badge;
  private String logo;
  private String address;
  private String website;
  private String intro;
  private Long allow;
  @TableField("addTime")
  private java.sql.Timestamp addTime;
  @TableField("createId")
  private Long createId;
  @TableField("oldPlatformId")
  private Long oldPlatformId;
  @TableField("useCourse")
  private Long useCourse;
  private Long cooperate;
  private Long sort;
  private String content;
  private String banner;
  private String contact;
  @JsonIgnore
  private String dbHost;
  @JsonIgnore
  private Long dbPort;
  @JsonIgnore
  private String dbName;
  @JsonIgnore
  private String dbUser;
  @JsonIgnore
  private String dbPass;
  private String copyright;
  private String map;
  private String domain;
  @TableField("domainType")
  private Long domainType;
  private String skin;

}
