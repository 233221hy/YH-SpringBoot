package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.YeeSchoolColumnDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeSchoolColumn;
import cn.xfywz.guozespring.entity.vo.YeeSchoolColumnListVO;
import cn.xfywz.guozespring.entity.vo.YeeSchoolColumnVO;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.service.teacher.YeeSchoolColumnService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import com.alibaba.fastjson2.JSON;
import cn.xfywz.guozespring.util.CosClientUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeeSchoolColumnServiceImpl implements YeeSchoolColumnService {

    private final DatabaseUtil databaseUtil;

    // ================ 辅助方法 ================

    /**
     * 构建查询条件
     */
    private void applyQueryConditions(QueryBuilder queryBuilder, YeeSchoolColumnDTO param) {
        if (StringUtils.hasText(param.getName())) {
            queryBuilder.like("name", param.getName());
        }
        if (param.getAllow() != null) {
            queryBuilder.eq("allow", param.getAllow());
        }
    }

    /**
     * 获取下一个sort值（默认从10开始，每次增加10）
     */
    private int getNextSortValue(int schoolId) {
        try {
            Optional<Integer> maxSort = databaseUtil.query(schoolId)
                    .sql("SELECT COALESCE(MAX(sort), 0) as maxSort FROM yee_school_column WHERE schoolId = ?")
                    .param(schoolId)
                    .single(rs -> {
                        try {
                            return rs.getInt("maxSort");
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });

            return maxSort.orElse(0) + 10;
        } catch (Exception e) {
            log.error("获取sort值失败: schoolId={}", schoolId, e);
            return 10; // 默认从10开始
        }
    }

    /**
     * 解析并规范化data字段，上传图片到COS
     */
    private String normalizeData(String dataJson) {
        log.debug("接收到的data数据: {}, 类型: {}", dataJson, dataJson != null ? dataJson.getClass().getName() : "null");

        try {
            if (dataJson == null || dataJson.trim().isEmpty()) {
                return "[]";
            }

            String trimmedData = dataJson.trim();

            // 检查是否为有效的JSON数组格式
            if (!trimmedData.startsWith("[") || !trimmedData.endsWith("]")) {
                log.error("data字段不是有效的JSON数组格式: {}", dataJson);
                return "[]";
            }

            List<Map> items = JSON.parseArray(trimmedData, Map.class);
            if (items == null) {
                return "[]";
            }

            List<Map<String, Object>> normalized = new ArrayList<>();
            for (Map<String, Object> item : items) {
                if (item == null) continue;

                // 处理图片字段
                Object imgObj = item.get("img");
                if (imgObj != null) {
                    String img = String.valueOf(imgObj);
                    if (!img.isEmpty() && !img.startsWith("http")) {
                        String cosUrl = tryUploadLocalFileToCos(img);
                        if (cosUrl != null) {
                            item.put("img", cosUrl);
                        }
                    }
                }
                normalized.add(item);
            }
            return JSON.toJSONString(normalized);
        } catch (Exception e) {
            log.error("data规范化失败: {}", e.getMessage(), e);
            return "[]";
        }
    }

//    // 解析 data 并将本地/临时路径图片上传到 COS，返回规范化 JSON 字符串
//    private String normalizeData(String dataJson) {
//        log.debug("接收到的data数据: {}, 类型: {}", dataJson, dataJson != null ? dataJson.getClass().getName() : "null");
//        try {
//            if (dataJson == null || dataJson.trim().isEmpty()) {
//                return "[]";
//            }
//
//            // 验证是否为有效的JSON格式
//            if (!isValidJson(dataJson)) {
//                log.error("无效的JSON格式: {}", dataJson);
//                return "[]";
//            }
//
//            String trimmedData = dataJson.trim();
//
//            // 检查是否为有效的JSON数组格式
//            if (trimmedData.startsWith("[") && trimmedData.endsWith("]")) {
//                List<Map> items = JSON.parseArray(trimmedData, Map.class);
//                return processItems(items);
//            } else {
//                log.error("data字段不是有效的JSON数组格式: {}", dataJson);
//                return "[]";
//            }
//
//        } catch (Exception e) {
//            log.error("data规范化失败: {}", e.getMessage(), e);
//            return "[]";
//        }
//    }

//    // 判断字符串是否为有效的JSON格式
//    private boolean isValidJson(String json) {
//        try {
//            JSON.parse(json);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private String processItems(List<Map> items) {
//        if (items == null) return "[]";
//
//        List<Map<String, Object>> normalized = new ArrayList<>();
//        for (Map<String, Object> item : items) {
//            if (item == null) continue;
//            Object imgObj = item.get("img");
//            String img = imgObj == null ? null : String.valueOf(imgObj);
//            if (img != null && !img.isEmpty() && !img.startsWith("http")) {
//                String fixed = img.trim();
//                fixed = fixed.replace(" ", "");
//                fixed = fixed.replace("\\", "/");
//                if (fixed.startsWith("/")) fixed = fixed.substring(1);
//                String url = tryUploadLocalFileToCos(fixed);
//                if (url != null && url.startsWith("http")) {
//                    item.put("img", url);
//                }
//            }
//            normalized.add(item);
//        }
//        return JSON.toJSONString(normalized);
//    }

//
//    // 读取本地文件并上传到 COS
//    private String tryUploadLocalFileToCos(String relativePath) {
//        try {
//            // 以当前工作目录为基准
//            String baseDir = System.getProperty("user.dir");
//            Path p = Paths.get(baseDir, relativePath.replace("/", File.separator));
//            if (!Files.exists(p)) {
//                // 兼容 upfiles 前缀
//                p = Paths.get(baseDir, "upfiles", relativePath.replace("/", File.separator));
//            }
//            if (!Files.exists(p)) {
//                // 再尝试去掉 upfiles
//                String rp = relativePath.replaceFirst("^upfiles/", "");
//                p = Paths.get(baseDir, rp.replace("/", File.separator));
//            }
//            if (!Files.exists(p)) {
//                return null;
//            }
//            byte[] bytes = Files.readAllBytes(p);
//            String filename = p.getFileName() != null ? p.getFileName().toString() : ("img_" + System.currentTimeMillis());
//            return CosClientUtil.uploadBytes(bytes, filename);
//        } catch (Exception e) {
//            return null;
//        }
//    }


    /**
     * 尝试上传本地文件到COS
     */
    private String tryUploadLocalFileToCos(String relativePath) {
        try {
            // 清理路径
            String fixedPath = relativePath.trim()
                    .replace(" ", "")
                    .replace("\\", "/")
                    .replaceFirst("^/", "");

            String baseDir = System.getProperty("user.dir");

            // 尝试多个可能的路径
            Path[] possiblePaths = {
                    Paths.get(baseDir, fixedPath),
                    Paths.get(baseDir, "upfiles", fixedPath),
                    Paths.get(baseDir, fixedPath.replaceFirst("^upfiles/", ""))
            };

            for (Path path : possiblePaths) {
                if (Files.exists(path)) {
                    byte[] bytes = Files.readAllBytes(path);
                    String filename = path.getFileName() != null ?
                            path.getFileName().toString() :
                            "img_" + System.currentTimeMillis() + ".jpg";

                    return CosClientUtil.uploadBytes(bytes, filename);
                }
            }

            log.warn("本地文件未找到: {}", relativePath);
            return null;
        } catch (Exception e) {
            log.warn("本地文件上传COS失败: {}", e.getMessage());
            return null;
        }
    }

    // ================ 业务方法 ================

    /**
     * 查询栏目列表
     */
    @Override
    public Result getColumnList(YeeSchoolColumnDTO param) {
        try {
            // 查询列表
            List<YeeSchoolColumnListVO> list = databaseUtil.query((int) param.getSchoolId())
                    .sql("SELECT * FROM yee_school_column WHERE 1=1")
                    .eq("schoolId", param.getSchoolId())
                    .apply(qb -> applyQueryConditions(qb, param))
                    .orderBy("sort ASC")
                    .list(YeeSchoolColumnListVO::fromResultSet);

            return Result.success(list);
        } catch (Exception e) {
            log.error("查询栏目列表失败: schoolId={}", param.getSchoolId(), e);
            throw new DatabaseException("查询栏目列表失败: " + e.getMessage(), e);
        }
    }


//    @Override
//    public Result getColumnList(YeeSchoolColumnDTO yeeSchoolColumnDTO) {
//        SlSchool slSchool = slSchoolMapper.selectById(yeeSchoolColumnDTO.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            return Result.error("学校不存在或未审核");
//        }
//
//        // 方法内本地类：负责 SQL 构造
//        class SqlBuilder {
//            BuiltSql buildList(YeeSchoolColumnDTO param) {
//                String baseSql = "select * from yee_school_column";
//                List<String> conditions = new ArrayList<>();
//                List<Object> ps = new ArrayList<>();
//                //必加
//                conditions.add("schoolId = ?");
//                ps.add(param.getSchoolId());
//                //可选
//                if (param.getName() != null && !param.getName().trim().isEmpty()) {
//                    conditions.add("name like ?");
//                    ps.add("%" + param.getName().trim() + "%");
//                }
//                if (param.getAllow() != null) {
//                    conditions.add("allow = ?");
//                    ps.add(param.getAllow());
//                }
//
//                return BuiltSql.of(baseSql + " WHERE " + String.join(" AND ", conditions), ps);
//
//            }
//        }
//
//        class DbExecutor {
//            List<YeeSchoolColumnListVO> queryList(SlSchool school, BuiltSql built) throws Exception {
//                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
//                     PreparedStatement st = connection.prepareStatement(built.sql)) {
//                    for (int i = 0; i < built.params.size(); i++) {
//                        st.setObject(i + 1, built.params.get(i));
//                    }
//                    try (ResultSet rs = st.executeQuery()) {
//                        List<YeeSchoolColumnListVO> list = new ArrayList<>();
//                        while (rs.next()) {
//                            list.add(YeeSchoolColumnListVO.fromResultSet(rs));
//                        }
//                        return list;
//                    }
//                }
//            }
//        }
//
//        SqlBuilder builder = new SqlBuilder();
//        DbExecutor executor = new DbExecutor();
//
//        try {
//            List<YeeSchoolColumnListVO> list = executor.queryList(slSchool, builder.buildList(yeeSchoolColumnDTO));
//            return Result.success(list);
//        } catch (Exception e) {
//            log.error("查询失败：{}", e.getMessage());
//            return Result.error("查询失败: " + e.getMessage());
//        }
//
//    }

    /**
     * 根据ID查询栏目详情
     */
    @Override
    public Result getById(YeeSchoolColumnDTO param) {
        try {
            Optional<YeeSchoolColumnVO> column = databaseUtil.query((int) param.getSchoolId())
                    .sql("SELECT * FROM yee_school_column WHERE id = ? AND schoolId = ?")
                    .param(param.getId())
                    .param(param.getSchoolId())
                    .single(YeeSchoolColumnVO::fromResultSet);

            return column.map(Result::success)
                    .orElseGet(() -> Result.error("栏目不存在"));
        } catch (Exception e) {
            log.error("查询栏目详情失败: id={}, schoolId={}", param.getId(), param.getSchoolId(), e);
            throw new DatabaseException("查询栏目详情失败: " + e.getMessage(), e);
        }
    }
//    @Override
//    public Result getById(YeeSchoolColumnDTO param) {
//        // 根据学校ID获取学校信息
//        SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            return Result.error("学校不存在或未审核");
//        }
//        class SqlBuilder {
//            BuiltSql buildGet() {
//                String sql = "SELECT * FROM yee_school_column WHERE id = ? AND schoolId = ?";
//                return BuiltSql.of(sql, Arrays.asList(param.getId(), param.getSchoolId()));
//            }
//        }
//        class DbExecutor {
//            YeeSchoolColumnVO queryOne(SlSchool school, BuiltSql built) throws Exception {
//                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
//                     PreparedStatement st = connection.prepareStatement(built.sql)) {
//                    for (int i = 0; i < built.params.size(); i++) {
//                        st.setObject(i + 1, built.params.get(i));
//                    }
//                    try (ResultSet rs = st.executeQuery()) {
//                        if (rs.next()) {
//                            return YeeSchoolColumnVO.fromResultSet(rs);
//                        }
//                        return null;
//                    }
//                }
//            }
//        }
//        SqlBuilder builder = new SqlBuilder();
//        DbExecutor executor = new DbExecutor();
//        try {
//            YeeSchoolColumnVO vo = executor.queryOne(slSchool, builder.buildGet());
//            return vo != null ? Result.success(vo) : Result.error("未找到记录");
//        } catch (Exception e) {
//            log.error("查询详情失败: {}", e.getMessage());
//            return Result.error("查询详情失败: " + e.getMessage());
//        }
//    }

    /**
     * 添加栏目
     */
    @Override
    public Result add(YeeSchoolColumn yeeSchoolColumn) {
        try {
            // 规范化data字段
            String normalizedData = normalizeData(JSON.toJSONString(yeeSchoolColumn.getData()));

            // 获取下一个sort值（如果未提供）
            Integer sortValue = yeeSchoolColumn.getSort();
            if (sortValue == null) {
                sortValue = getNextSortValue(yeeSchoolColumn.getSchoolId());
            }

            // 插入数据
            Long generatedId = databaseUtil.update(yeeSchoolColumn.getSchoolId())
                    .table("yee_school_column")
                    .set("schoolId", yeeSchoolColumn.getSchoolId())
                    .set("name", yeeSchoolColumn.getName())
                    .set("addTime", yeeSchoolColumn.getAddTime() != null ?
                            yeeSchoolColumn.getAddTime() : new Timestamp(System.currentTimeMillis()))
                    .set("allow", yeeSchoolColumn.getAllow() != null ?
                            yeeSchoolColumn.getAllow() : 1)
                    .set("type", yeeSchoolColumn.getType() != null ?
                            yeeSchoolColumn.getType() : 1)
                    .set("sort", sortValue)
                    .set("data", normalizedData)
                    .setIfNotEmpty("more", yeeSchoolColumn.getMore())
                    .insert();

            if (generatedId != null) {

                return Result.success("添加成功");
            } else {
                return Result.error("添加失败");
            }
        } catch (Exception e) {
            throw new DatabaseException("添加栏目失败: " + e.getMessage(), e);
        }
    }

//    @Override
//    public Result add(YeeSchoolColumn yeeSchoolColumn) {
//        // 根据学校ID获取学校信息
//        SlSchool slSchool = slSchoolMapper.selectById(yeeSchoolColumn.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            return Result.error("学校不存在或未审核");
//        }
//
//        class SqlBuilder {
//            BuiltSql buildAdd(YeeSchoolColumn param) {
//                LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
//                fields.put("schoolId", param.getSchoolId());
//                fields.put("name", param.getName());
//                fields.put("addTime", param.getAddTime() != null ? param.getAddTime() : new Timestamp(System.currentTimeMillis()));
//                fields.put("allow", param.getAllow() != null ? param.getAllow() : 1);
//                fields.put("type", param.getType() != null ? param.getType() : 1);
//                //权重值
//                Integer sortValue = param.getSort();
//                if (sortValue == null) {
//                    // 需要查询数据库获取当前最大sort值
//                    sortValue = getNextSortValue(param.getSchoolId());
//                }
//                fields.put("sort", sortValue);
//
//                // 将 data 解析并上传图片到 COS，统一写入规范化 JSON
//                fields.put("data", normalizeData(JSON.toJSONString(param.getData())));
//                String cols = fields.keySet().stream().map(k -> "`" + k + "`").collect(Collectors.joining(", "));
//                String placeholders = fields.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
//                String baseSql = "insert into yee_school_column (" + cols + ") values (" + placeholders + ")";
//
//                return BuiltSql.of(baseSql, new ArrayList<>(fields.values()));
//            }
//
//            // 获取下一个sort值
//            private Integer getNextSortValue(Integer schoolId) {
//                String sql = "SELECT COALESCE(MAX(sort), 0) + 10 as nextSort FROM yee_school_column WHERE schoolId = ?";
//                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//                     PreparedStatement st = connection.prepareStatement(sql)) {
//                    st.setInt(1, schoolId);
//                    try (ResultSet rs = st.executeQuery()) {
//                        if (rs.next()) {
//                            return rs.getInt("nextSort");
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("获取sort值失败: {}", e.getMessage());
//                }
//                return 10; // 默认从10开始
//            }
//        }
//
//        class DbExecutor {
//            int execute(SlSchool school, BuiltSql built) throws Exception {
//                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
//                     PreparedStatement st = connection.prepareStatement(built.sql)) {
//                    for (int i = 0; i < built.params.size(); i++) {
//                        st.setObject(i + 1, built.params.get(i));
//                    }
//                    return st.executeUpdate();
//                }
//            }
//        }
//
//        SqlBuilder builder = new SqlBuilder();
//        DbExecutor executor = new DbExecutor();
//
//        try {
//            int result = executor.execute(slSchool, builder.buildAdd(yeeSchoolColumn));
//            return result > 0 ? Result.success() : Result.error("添加失败");
//        } catch (Exception e) {
//            log.error("添加失败: {}", e.getMessage());
//            return Result.error("添加失败: " + e.getMessage());
//        }
//
//    }

    /**
     * 更新栏目
     */
    @Override
    public Result update(YeeSchoolColumn yeeSchoolColumn) {
        try {
            // 特殊处理data字段
            String normalizedData;
            if (yeeSchoolColumn.getData() != null) {
                normalizedData = normalizeData(JSON.toJSONString(yeeSchoolColumn.getData()));
            } else {
                normalizedData = null;
            }

            // 构建更新参数并执行更新
            int rows = databaseUtil.update(yeeSchoolColumn.getSchoolId())
                    .table("yee_school_column")
                    .setIfNotEmpty("name", yeeSchoolColumn.getName())
                    .setIfNotEmpty("more", yeeSchoolColumn.getMore())
                    .setIfNotNull("type", yeeSchoolColumn.getType())
                    .setIfNotNull("allow", yeeSchoolColumn.getAllow())
                    .setIfNotNull("sort", yeeSchoolColumn.getSort())
                    .apply(builder -> {
                        // 特殊处理data字段
                        if (normalizedData != null) {
                            builder.set("data", normalizedData);
                        }
                    })
                    .eq("id", yeeSchoolColumn.getId())
                    .eq("schoolId", yeeSchoolColumn.getSchoolId())
                    .update();

            if (rows > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败，栏目不存在或数据未变化");
            }
        } catch (Exception e) {
            log.error("更新栏目失败: id={}, schoolId={}",
                    yeeSchoolColumn.getId(), yeeSchoolColumn.getSchoolId(), e);
            throw new DatabaseException("更新栏目失败: " + e.getMessage(), e);
        }
    }
//    @Override
//    public Result update(YeeSchoolColumn yeeSchoolColumn) {
//        // 根据学校ID获取学校信息
//        SlSchool slSchool = slSchoolMapper.selectById(yeeSchoolColumn.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            return Result.error("学校不存在或未审核");
//        }
//
//        class SqlBuilder {
//            BuiltSql buildUpdate(YeeSchoolColumn param) {
//                LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
//                if (param.getName() != null) fields.put("name", param.getName());
//                if (param.getType() != null) fields.put("type", param.getType());
//                if (param.getMore() != null) fields.put("more", param.getMore());
//                if (param.getAllow() != null) fields.put("allow", param.getAllow());
//                if (param.getSort() != null) fields.put("sort", param.getSort());
//                if (param.getData() != null) fields.put("data", normalizeData(JSON.toJSONString(param.getData())));
//                if (fields.isEmpty()) return null;
//                String setClause = fields.keySet().stream()
//                        .map(k -> "`" + k + "` = ?")
//                        .collect(Collectors.joining(", "));
//                String sql = "UPDATE yee_school_column SET " + setClause + " WHERE id = ?";
//                List<Object> params = new ArrayList<>(fields.values());
//                params.add(param.getId());
//                return BuiltSql.of(sql, params);
//            }
//        }
//        class DbExecutor {
//            int execute(SlSchool school, BuiltSql built) throws Exception {
//                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
//                     PreparedStatement st = connection.prepareStatement(built.sql)) {
//                    for (int i = 0; i < built.params.size(); i++) {
//                        st.setObject(i + 1, built.params.get(i));
//                    }
//                    return st.executeUpdate();
//                }
//            }
//        }
//
//        SqlBuilder builder = new SqlBuilder();
//        DbExecutor executor = new DbExecutor();
//        try {
//            int result = executor.execute(slSchool, builder.buildUpdate(yeeSchoolColumn));
//            return result > 0 ? Result.success() : Result.error("更新失败");
//        } catch (Exception e) {
//            log.error("更新失败: {}", e.getMessage());
//            return Result.error("更新失败: " + e.getMessage());
//        }
//    }

    /**
     * 删除栏目
     */
    @Override
    public Result delete(YeeSchoolColumnDTO param) {
        try {
            int rows = databaseUtil.update((int) param.getSchoolId())
                    .table("yee_school_column")
                    .eq("id", param.getId())
                    .eq("schoolId", param.getSchoolId())
                    .delete();

            if (rows > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败，栏目不存在");
            }
        } catch (Exception e) {
            log.error("删除栏目失败: id={}, schoolId={}", param.getId(), param.getSchoolId(), e);
            throw new DatabaseException("删除栏目失败: " + e.getMessage(), e);
        }
    }
//    @Override
//    public Result delete(YeeSchoolColumnDTO param) {
//        // 根据学校ID获取学校信息
//        SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            return Result.error("学校不存在或未审核");
//        }
//        class SqlBuilder {
//            BuiltSql buildDelete(YeeSchoolColumnDTO param) {
//                String sql = "DELETE FROM yee_school_column WHERE id = ?";
//                return BuiltSql.of(sql, param.getId());
//            }
//        }
//        class DbExecutor {
//            int execute(SlSchool school, BuiltSql built) throws Exception {
//                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
//                     PreparedStatement st = connection.prepareStatement(built.sql)) {
//                    for (int i = 0; i < built.params.size(); i++) {
//                        st.setObject(i + 1, built.params.get(i));
//                    }
//                    return st.executeUpdate();
//                }
//            }
//        }
//        SqlBuilder builder = new SqlBuilder();
//        DbExecutor executor = new DbExecutor();
//        try {
//            int result = executor.execute(slSchool, builder.buildDelete(param));
//            return result > 0 ? Result.success() : Result.error("删除失败");
//        } catch (Exception e) {
//            log.error("删除失败: {}", e.getMessage());
//            return Result.error("删除失败: " + e.getMessage());
//        }
//    }

}


