package cn.xfywz.guozespring.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * @Author: ChengLin
 */
public class ParseJsonUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 工具方法1：解析 JSON 到 Map<String, Object>
    public Map<String, Object> parseJsonToMap(String json, String field, Integer questionId) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            System.err.printf("❌ [%s] Failed to parse JSON for field '%s', questionId=%d, raw JSON='%s'%n",
                    this.getClass().getSimpleName(), field, questionId, json);
            e.printStackTrace();
            return null; // 或返回空 map: new HashMap<>()
        }
    }

    // 工具方法2：解析 JSON 到 List<Map<String, Object>>
    public List<Map<String, Object>> parseJsonToList(String json, String field, Integer questionId) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return null;
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            System.err.printf("❌ [%s] Failed to parse JSON List for field '%s', questionId=%d, raw JSON='%s'%n",
                    this.getClass().getSimpleName(), field, questionId, json);
            e.printStackTrace();
            return null;
        }
    }

    // 工具方法3：解析 JSON 到 List<Integer>
    public List<Integer> parseJsonToIntegerList(String json, String field, Integer questionId) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return null;
        }
        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            System.err.printf("❌ [%s] Failed to parse JSON Integer List for field '%s', questionId=%d, raw JSON='%s'%n",
                    this.getClass().getSimpleName(), field, questionId, json);
            e.printStackTrace();
            return null;
        }
    }
}
