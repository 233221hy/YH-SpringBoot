package cn.xfywz.guozespring.entity.file;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AsyncQueryTask {
    private String taskId;
    private String status;
    private List<Map<String, Object>> data;
//    private byte[] fileBytes;    // 导出的ZIP文件
    private String fileName;    // 文件名
    private String errorMsg;
    private Integer total;       // 总学生数
    private Integer current;     // 当前已处理数量
    private String filePath; // 用来存ZIP文件在服务器磁盘上的路径
}