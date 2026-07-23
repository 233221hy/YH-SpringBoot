package cn.xfywz.guozespring.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * @Author: ChengLin
 */
public class JsonUtil {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    // 修改你的 JsonUtil 工具类，添加一个专用方法
    public <T> List<Map<String, T>> fromJsonToListOfMap(String json) {
        JavaType type = objectMapper.getTypeFactory()
                .constructCollectionType(List.class,
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    private static final Gson GSON = new Gson();

    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return null;
        Type type = TypeToken.getParameterized(List.class, clazz).getType();
        return GSON.fromJson(json, type);
    }

    public static List<Map<String, Object>> parseOption(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        return GSON.fromJson(json, type);
    }


//    ===========
    public static Object parse(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            if (json.startsWith("{")) {
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                return GSON.fromJson(json, mapType);
            } else if (json.startsWith("[")) {
                Type listType = new TypeToken<List<Object>>(){}.getType();
                return GSON.fromJson(json, listType);
            } else {
                return GSON.fromJson(json, Object.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败: " + json, e);
        }
    }

    public static String toJson(Object obj) {
        return obj == null ? null : GSON.toJson(obj);
    }

}

