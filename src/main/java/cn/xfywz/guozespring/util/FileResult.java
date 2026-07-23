package cn.xfywz.guozespring.util;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 */
@Data
public class FileResult<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <T> FileResult<T> success() {
        FileResult<T> result = new FileResult<>();
        result.code = 200;
        result.message = "成功";
        return result;
    }

    public static <T> FileResult<T> success(T data) {
        FileResult<T> result = new FileResult<>();
        result.code = 200;
        result.message = "成功";
        result.data = data;
        return result;
    }

    public static <T> FileResult<T> error(String message) {
        FileResult<T> result = new FileResult<>();
        result.code = 500;
        result.message = message;
        return result;
    }

    public static <T> FileResult<T> error(int code, String message) {
        FileResult<T> result = new FileResult<>();
        result.code = code;
        result.message = message;
        return result;
    }
}