package cn.xfywz.guozespring.entity.dto;

import lombok.Value;

/**
 * 学生姓名和学号
 */
@Value
public class StuNameAndNum {

    String name;
    String number;

    public StuNameAndNum(String name, String number) {
        this.name = name == null ? "" : name;
        this.number = number == null ? "" : number;
    }
}
