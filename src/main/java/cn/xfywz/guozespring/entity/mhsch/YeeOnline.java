package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 在线记录
 * @TableName yee_online
 */
@TableName(value ="yee_online")
@Data
public class YeeOnline {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 登录账号
     */
    private Integer userId;

    /**
     * 登录时间
     */
    private Integer logInTime;

    /**
     * 最后活跃时间
     */
    private Integer lastTime;

    /**
     * 平台
     */
    private String platform;

    /**
     * IP
     */
    private Integer ip;

    /**
     * 时长
     */
    private Integer duration;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     * 
     */
    private Date loginTime2;

    /**
     * 
     */
    private Date lastTime2;

    /**
     * 
     */
    private String ip2;
}