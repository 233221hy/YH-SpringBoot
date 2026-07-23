package cn.xfywz.guozespring.entity.dto;

import lombok.Data;

@Data
public class YeeManageEditDTO {
    /**
     * 姓名
     */
    private String name;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 手机
     */
    private String mobile;
    /**
     * 角色
     */
    private Integer role;
    /**
     * 状态
     */
    private Integer active;
    /**
     * 所属学院
     */
    private Integer collegeId;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 性别
     */
    private String gender;
    /**
     * 微信
     */
    private String weChat;
    /**
     * 简介
     */
    private String intro;
    /**
     * 兼职学院
     */
    private String colleges;
    /**
     * 推荐
     */
    private Integer recommend;
    /**
     * 绑定微信
     */
    private Integer force;
    /**
     * 通行密钥
     */
    private String passport;

}
