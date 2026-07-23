package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class SlManageNode {
    private long id;
    private long pid;
    private String name;
    private String controller;
    private String action;
    private String args;
    private long sort;
    @TableField("mainBar")
    private long mainBar;
    
    // 树形结构子节点
    @TableField(exist = false)
    private List<SlManageNode> children;
}