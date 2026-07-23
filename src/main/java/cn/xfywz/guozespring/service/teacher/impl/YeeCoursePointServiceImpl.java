package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.excel.ExcelExportStyles;
import cn.xfywz.guozespring.excel.ResponseExportUtil;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeCoursePointService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;

import static cn.xfywz.guozespring.excel.ExcelDataPreprocessor.safeReplaceComma;

/**
 * @Author: ChengLin
 */
@Service
public class YeeCoursePointServiceImpl implements YeeCoursePointService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result selectAll(int schoolId, Integer courseId, String title, Integer classId) throws Exception {

        // 查询学生:姓名,学号
        List<Map<String, Object>> userList = getWorkDetails(schoolId, courseId, classId, title);

        // 该课程下所有学生的积分信息
        List<Map<String, Object>> pointList = yeeCoursePointList(schoolId, courseId);

        // 构建结果 使用stream 根据 userList中的id 和 pointList 中的 studentId 进行匹配 把数据封装成 Map<String, Object>
        List<Map<String, Object>> result = mergeUserWithPointAndSort(userList, pointList);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("result", result);
//        resultMap.put("userList", userList);
//        resultMap.put("pointList", pointList);

        return Result.success(resultMap);
    }

    @Override
    public Result selectTodayAll(int schoolId, Integer courseId, String title, Integer classId, String date) throws Exception {
        // 查询学生:姓名,学号
        List<Map<String, Object>> userList = getWorkDetails(schoolId, courseId, classId, title);

        // 该课程下今天(某天)学生的积分信息
        List<Map<String, Object>> pointList = yeeCoursePointTodayList(schoolId, courseId, date);

        // 构建结果 使用stream 根据 userList中的id 和 pointList 中的 studentId 进行匹配 把数据封装成 Map<String, Object>
        List<Map<String, Object>> result = mergeUserWithPointAndSortToday(userList, pointList);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("result", result);
//        resultMap.put("userList", userList);
//        resultMap.put("pointList", pointList);

        return Result.success(resultMap);
    }

    @Override
    public Result selectMonthAll(int schoolId, Integer courseId, String title, Integer classId, String date) throws Exception {
        // 查询学生:姓名,学号
        List<Map<String, Object>> userList = getWorkDetails(schoolId, courseId, classId, title);

        // 该课程下今天(某天)学生的积分信息
        List<Map<String, Object>> pointList = yeeCoursePointMonthList(schoolId, courseId, date);

        // 构建结果 使用stream 根据 userList中的id 和 pointList 中的 studentId 进行匹配 把数据封装成 Map<String, Object>
        List<Map<String, Object>> result = mergeUserWithPointAndSortToday(userList, pointList);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("result", result);
//        resultMap.put("userList", userList);
//        resultMap.put("pointList", pointList);

        return Result.success(resultMap);
    }

    private List<Map<String, Object>> yeeCoursePointMonthList(
            Integer schoolId,
            Integer courseId,
            String date           // 接收字符串格式的日期，如 "2025-09-18"
    ) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT
                cpm.id,
                cpm.courseId,
                cpm.month,
                cpm.studentId,
                cpm.point,
                cpm.rank,
                cpm.point2
            FROM yee_course_point_month cpm
            WHERE cpm.courseId = ?
            """);

            List<Object> parameters = new ArrayList<>();
            parameters.add(courseId);

            // 如果 date 不为空，则添加日期条件
            if (date != null && !date.trim().isEmpty()) {
                try {
                    sqlBuilder.append(" AND cpm.month = ? ");
                    parameters.add(date);
                } catch (DateTimeParseException e) {
                    throw new Exception("日期格式不正确，正确格式：yyyy-MM-dd，当前值：" + date);
                }
            }
            // 如果 date 为空，不加条件，相当于查全部

            sqlBuilder.append(" ORDER BY cpm.point DESC ");

            // 4. 预编译
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);

                    if ("month".equals(columnName)) {
                        row.put("month", rs.getObject(i).toString());
                    } else {
                        row.put(columnName, rs.getObject(i));
                    }


                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询课程下学生信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId +
                    ", date=" + date);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> yeeCoursePointTodayList(
            Integer schoolId,
            Integer courseId,
            String date           // 接收字符串格式的日期，如 "2025-09-18"
    ) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT
                cpm.id,
                cpm.courseId,
                cpm.date,
                cpm.studentId,
                cpm.point,
                cpm.rank,
                cpm.point2
            FROM yee_course_point_days cpm
            WHERE cpm.courseId = ?
            """);

            List<Object> parameters = new ArrayList<>();
            parameters.add(courseId);

            // 如果 date 不为空，则添加日期条件
            if (date != null && !date.trim().isEmpty()) {
                try {
                    sqlBuilder.append(" AND cpm.date = ? ");
                    parameters.add(date);
                } catch (DateTimeParseException e) {
                    throw new Exception("日期格式不正确，正确格式：yyyy-MM-dd，当前值：" + date);
                }
            }
            // 如果 date 为空，不加条件，相当于查全部

            sqlBuilder.append(" ORDER BY cpm.point DESC ");

            // 4. 预编译
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);

                    if ("date".equals(columnName)) {
                        row.put("date", rs.getObject(i).toString());
                    } else {
                        row.put(columnName, rs.getObject(i));
                    }


                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询课程下学生信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId + ", date=" + date);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    public List<Map<String, Object>> mergeUserWithPointAndSortToday(
            List<Map<String, Object>> userList,
            List<Map<String, Object>> pointList) {

        // 构建 studentId -> point 的映射
        Map<Long, Map<String, Object>> pointMap = pointList.stream()
                .collect(Collectors.toMap(
                        point -> ((Number) point.get("studentId")).longValue(),
                        point -> point,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return userList.stream()
                // 1. 先 map：合并数据
                .map(user -> {
                    Map<String, Object> merged = new HashMap<>(user);
                    Long userId = user.get("id") == null ? null : ((Number) user.get("id")).longValue();

                    if (userId != null) {
                        Map<String, Object> point = pointMap.get(userId);
                        if (point != null) {
                            merged.put("point", point.get("point"));
                            merged.put("rank", point.get("rank"));
                            merged.put("point2", point.get("point2"));
                            merged.put("month", point.get("month"));
                        } else {
                            merged.put("point", null);
                            merged.put("rank", null);
                            merged.put("point2", null);
                            merged.put("month", null);
                        }
                    } else {
                        merged.put("point", null);
                        merged.put("rank", null);
                        merged.put("point2", null);
                        merged.put("month", null);
                    }

                    return merged;
                })
                // 2. 再 sorted：按 point 降序，null 放最后
                .sorted((map1, map2) -> {
                    Integer point1 = (Integer) map1.get("point");
                    Integer point2 = (Integer) map2.get("point");

                    if (point1 == null && point2 == null) return 0;
                    if (point1 == null) return 1;      // null 排后面
                    if (point2 == null) return -1;     // null 排后面
                    return point2.compareTo(point1);   // 降序：大的在前
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> mergeUserWithPointAndSort(
            List<Map<String, Object>> userList,
            List<Map<String, Object>> pointList) {

        // 构建 studentId -> point 的映射
        Map<Long, Map<String, Object>> pointMap = pointList.stream()
                .collect(Collectors.toMap(
                        point -> ((Number) point.get("studentId")).longValue(),
                        point -> point,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return userList.stream()
                // 1. 先 map：合并数据
                .map(user -> {
                    Map<String, Object> merged = new HashMap<>(user);
                    Long userId = user.get("id") == null ? null : ((Number) user.get("id")).longValue();

                    if (userId != null) {
                        Map<String, Object> point = pointMap.get(userId);
                        if (point != null) {
                            merged.put("point", point.get("point"));
                            merged.put("rank", point.get("rank"));
                            merged.put("point2", point.get("point2"));
                        } else {
                            merged.put("point", null);
                            merged.put("rank", null);
                            merged.put("point2", null);
                        }
                    } else {
                        merged.put("point", null);
                        merged.put("rank", null);
                        merged.put("point2", null);
                    }

                    return merged;
                })
                // 2. 再 sorted：按 point 降序，null 放最后
                .sorted((map1, map2) -> {
                    Integer point1 = (Integer) map1.get("point");
                    Integer point2 = (Integer) map2.get("point");

                    if (point1 == null && point2 == null) return 0;
                    if (point1 == null) return 1;      // null 排后面
                    if (point2 == null) return -1;     // null 排后面
                    return point2.compareTo(point1);   // 降序：大的在前
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> yeeCoursePointList(Integer schoolId, Integer courseId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建动态 SQL：查询课程下的学生信息（姓名、学号、班级等）
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
                    SELECT
                              cpm.id,
                              cpm.courseId,
                              cpm.`date`,
                              cpm.studentId,
                              cpm.`point`,
                              cpm.rank,
                              cpm.point2
                        FROM yee_course_point_days cpm
                    
                        WHERE cpm.courseId = ?
                        
                   
                """);

            sqlBuilder.append("""
                ORDER BY cpm.point DESC
                """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, courseId);

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    row.put(columnName, rs.getObject(i));
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询课程下学生信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }


    private List<Map<String, Object>> getWorkDetails(
            Integer schoolId,
            Integer courseId,
            Integer classId,
            String title) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建动态 SQL：查询课程下的学生信息（姓名, 学号, 班级, 性别）
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
                    SELECT 
                        s.number AS number,
                        s.`name` AS `name`,
                        cc.`name` AS className,
                        s.gender AS gender,
                        s.id
                    FROM 
                        yee_course_student cs
                        LEFT JOIN yee_student s ON s.id = cs.studentId
                        LEFT JOIN yee_course_class cc on cc.id = cs.classId
                    WHERE 
                        cs.courseId = ?
                    """);

            // 条件：classId 可选
            if (classId != null && classId > 0) {
                sqlBuilder.append(" AND cs.classId = ? ");
            }

            // 条件：title 支持模糊匹配 name 或 number（学号）
            if (title != null && !title.trim().isEmpty()) {
                String trimmedTitle = "%" + title.trim() + "%";
                sqlBuilder.append(" AND (s.name LIKE ? OR s.number LIKE ?) ");
            }

            sqlBuilder.append("""
                ORDER BY 
                    cs.classId, 
                    s.name
                """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, courseId);

            if (classId != null && classId > 0) {
                st.setInt(paramIndex++, classId);
            }

            if (title != null && !title.trim().isEmpty()) {
                String likeValue = "%" + title.trim() + "%";
                st.setString(paramIndex++, likeValue); // 匹配 name
                st.setString(paramIndex++, likeValue); // 匹配 number
            }

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    row.put(columnName, rs.getObject(i));
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询课程下学生信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId +
                    ", classId=" + classId +
                    ", title=" + title, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }


    // ---------------- 工具方法 ----------------

    /**
     * 安全关闭 ResultSet 和 PreparedStatement
     */
    private void closeResultSetAndStatement(ResultSet rs, PreparedStatement stmt) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace(); // 建议使用日志框架
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 安全关闭 Connection
     */
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ======== 导出：全部/今日/本月 ========
    @Override
    public void exportAll(int schoolId, Integer courseId, String title, Integer classId, HttpServletResponse response) throws Exception {
        // 查询学生基本信息 + 当前积分（全部）
        List<Map<String, Object>> userList = getWorkDetails(schoolId, courseId, classId, title);
        List<Map<String, Object>> pointList = yeeCoursePointList(schoolId, courseId);
        List<Map<String, Object>> rows = mergeUserWithPointAndSort(userList, pointList);
        writeRankingExcel(response, rows, "全部");
    }

    @Override
    public void exportToday(int schoolId, Integer courseId, String title, Integer classId, String date, HttpServletResponse response) throws Exception {
        List<Map<String, Object>> userList = getWorkDetails(schoolId, courseId, classId, title);
        List<Map<String, Object>> pointList = yeeCoursePointTodayList(schoolId, courseId, date);
        List<Map<String, Object>> rows = mergeUserWithPointAndSortToday(userList, pointList);
        String suffix = (date != null && !date.trim().isEmpty()) ? date.trim() : "今日";
        writeRankingExcel(response, rows, suffix);
    }

    @Override
    public void exportMonth(int schoolId, Integer courseId, String title, Integer classId, String date, HttpServletResponse response) throws Exception {
        List<Map<String, Object>> userList = getWorkDetails(schoolId, courseId, classId, title);
        List<Map<String, Object>> pointList = yeeCoursePointMonthList(schoolId, courseId, date);
        List<Map<String, Object>> rows = mergeUserWithPointAndSortToday(userList, pointList);
        String suffix = (date != null && !date.trim().isEmpty()) ? date.trim() : "本月";
        writeRankingExcel(response, rows, suffix);
    }

    /** 写 Excel（学生积分排行榜） */
    private void writeRankingExcel(HttpServletResponse response, List<Map<String, Object>> rows, String timeSuffix) throws Exception {
        // 文件名：学生积分排行榜_时间.xlsx
//        ResponseExportUtil.setExcelRespProp(response, "学生积分排行榜_" + timeSuffix);
        ResponseExportUtil.setExcelRespProp(response, "学生积分排行榜_" + System.currentTimeMillis());
        String dateStr = new SimpleDateFormat("yyyy年MM月dd日").format(new Date());
        String title = "学生积分排行榜—（" + dateStr + " 导出）";
        String[] headers = new String[]{"排名","学号","姓名","性别","教学班级","积分"};

        List<List<String>> head = new ArrayList<>();
        for (String h : headers) head.add(Arrays.asList(title, h));

        // 组装数据行
        List<List<String>> dataRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> m = rows.get(i);
            // 排名优先使用库里的 rank 字段，否则按排序顺序+1
            Object rankObj = m.get("rank");
            String rankStr = (rankObj != null) ? String.valueOf(rankObj) : String.valueOf(i + 1);
            String number = safeReplaceComma(String.valueOf(m.get("number")));
            String name = safeReplaceComma(String.valueOf(m.get("name")));
            String gender = safeReplaceComma(String.valueOf(m.get("gender")));
            String className = safeReplaceComma(String.valueOf(m.get("className")));
            String pointStr = safeReplaceComma(String.valueOf(m.get("point")));

            List<String> row = new ArrayList<>(headers.length);
            row.add(rankStr);
            row.add(number);
            row.add(name);
            row.add(gender);
            row.add(className);
            row.add(pointStr);
            dataRows.add(row);
        }

        HorizontalCellStyleStrategy styleStrategy = ExcelExportStyles.defaultStyleStrategy();
        try {
            EasyExcel.write(response.getOutputStream())
                    .autoCloseStream(false)
                    .head(head)
                    .registerWriteHandler(styleStrategy)
                    .registerWriteHandler(ExcelExportStyles.defaultTitleRow(headers.length))
                    .registerWriteHandler(new OnceAbsoluteMergeStrategy(0, 0, 0, headers.length - 1))
//                    .registerWriteHandler(ExcelExportStyles.createFreezeAndWidthHandler(new int[]{8, 20, 16, 10, 18, 10}, 2))
//                    .registerWriteHandler(ExcelExportStyles.textColumns(new int[]{1})) // 学号列文本
                    .sheet("积分排行")
                    .doWrite(dataRows);
            response.flushBuffer();
        } catch (Exception e) {
            // 写失败时返回错误信息
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("导出失败:" + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

}
