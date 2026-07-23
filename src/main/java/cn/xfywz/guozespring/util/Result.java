package cn.xfywz.guozespring.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor// cn.xfywz.guozespring.util.Result
public class Result {
    private int code;
    private String msg;
    private Object data;
    private Long count;

    // 新增：用于存放额外信息，如 studentStats
    private Map<String, Object> extra;

    // 新增：list 的便捷方法
    private List list;

    // 构造方法
    public static Result success(Object data) {
        Result result = new Result();
        result.setCode(200);
        result.setData(data);
        result.setMsg("success");
        result.setExtra(new HashMap<>()); // 初始化
        return result;
    }

    public static Result success() {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        result.setData("操作成功");
        result.setExtra(new HashMap<>());
        return result;
    }

    public static Result success(String msg, Object data) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        result.setExtra(new HashMap<>());
        return result;
    }

    public static Result success(Object data, Long count) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        result.setCount(count);
        result.setData(data);
        result.setExtra(new HashMap<>());
        return result;
    }

    // ✅ 新增：专门用于返回 List 的方法（避免歧义）
    public static Result successWithList(Object data, List list) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        result.setList(list);
        return result;
    }

    // 新增：支持 extra 的 success 方法
    public static Result success(Object data, List list) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        result.setList(list);
        return result;
    }

    public static Result success(Object data, Long count, Map<String, Object> extra) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        result.setCount(count);
        result.setExtra(extra);
        return result;
    }

    public static Result success(Object data, Map<String, Object> extra) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        result.setCount(null);
        result.setExtra(extra);
        return result;
    }

    // error 方法保持不变，也加上 extra 初始化

    public static Result error(Object data) {
        Result result = error();
        result.setData(data);
        return result;
    }

    public static Result error() {
        Result result = new Result();
        result.setCode(500);
        result.setMsg("error");
        result.setExtra(new HashMap<>()); // 保持一致
        return result;
    }

    public static Result error(String msg, Object data) {
        Result result = new Result();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(data);
        result.setExtra(new HashMap<>());
        return result;
    }

    // 链式添加 extra 的便捷方法
    public Result extra(String key, Object value) {
        if (this.extra == null) {
            this.extra = new HashMap<>();
        }
        this.extra.put(key, value);
        return this;
    }
}



