package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 试题篮筐实体类
 * 对应数据库表: yee_basket
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeBasket implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer userId;
    private Integer type;
    private Integer exId;
    private Integer score;
    private Integer schoolId;
    private Byte remote;
}