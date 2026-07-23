package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeNotice;
import cn.xfywz.guozespring.entity.vo.YeeNoticeVo;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeNoticeService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程消息通知管理
 * select：根据课程ID查询通知列表
 * add：添加新通知
 * update：更新通知信息
 * delete：删除指定通知
 * search：按标题搜索通知
 * selectById：根据ID查询通知详情
 * selectList：学生端获取消息列表（支持分页和已读/未读状态筛选）
 */

@Service
public class YeeNoticeServiceImpl implements YeeNoticeService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private YeeNotice rsToYeeNotice(ResultSet rs) throws SQLException {
        YeeNotice yeeNotice = new YeeNotice();
        yeeNotice.setId(rs.getLong("id"));
        yeeNotice.setCourseId(rs.getLong("courseId"));
        yeeNotice.setType(rs.getLong("type"));

        // 解析 classIds 字符串为 List<Integer>
        String classIdsStr = rs.getString("classIds");
        List<Integer> classIds = Collections.emptyList();
        if (classIdsStr != null && !classIdsStr.trim().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                classIds = objectMapper.readValue(classIdsStr, new TypeReference<List<Integer>>() {});
            } catch (Exception e) {
                // 日志记录或默认空列表
                classIds = Collections.emptyList();
            }
        }
        yeeNotice.setClassIds(classIds);

        yeeNotice.setUserNumber(rs.getString("userNumber"));
        yeeNotice.setTitle(rs.getString("title"));
        yeeNotice.setSummary(rs.getString("summary"));
        yeeNotice.setContent(rs.getString("content"));
        yeeNotice.setUserId(rs.getLong("userId"));
        yeeNotice.setAddTime(rs.getTimestamp("addTime"));
        yeeNotice.setIsPush(rs.getLong("isPush"));
        yeeNotice.setPushTime(rs.getTimestamp("pushTime"));
        yeeNotice.setSysPush(rs.getLong("sysPush"));
        yeeNotice.setSchoolId(rs.getLong("schoolId"));
        return yeeNotice;
    }
    @Override
    public Result teacherSelect(int schoolId, String title, Integer type, Long courseId, int pageNum, int pageSize) {
        Connection connection = null;
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 构建基础查询
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_notice WHERE schoolId = ?");
            List<Object> params = new ArrayList<>();
            params.add(schoolId);

            if (courseId != null && courseId > 0) {
                sqlBuilder.append(" AND courseId = ?");
                params.add(courseId);
            }
            if (type != null && type >= 0) {
                sqlBuilder.append(" AND type = ?");
                params.add(type);
            }
            if (title != null && !title.trim().isEmpty()) {
                sqlBuilder.append(" AND title LIKE ?");
                params.add("%" + title.trim() + "%");
            }

            // 查询总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS tmp";
            PreparedStatement countSt = connection.prepareStatement(countSql);
            for (int i = 0; i < params.size(); i++) {
                countSt.setObject(i + 1, params.get(i));
            }
            ResultSet countRs = countSt.executeQuery();
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            countRs.close();
            countSt.close();

            // 分页查询
            sqlBuilder.append(" ORDER BY addTime DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((pageNum - 1) * pageSize);

            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }

            ResultSet rs = st.executeQuery();
            List<YeeNotice> noticeList = new ArrayList<>();
            while (rs.next()) {
                YeeNotice notice = rsToYeeNotice(rs);
                noticeList.add(notice);
            }
            rs.close();
            st.close();

            // ====== 提取所有 classId 和 courseId 并去重 ======
            Set<Integer> classIdSet = new HashSet<>();
            Set<Long> courseIdSet = new HashSet<>();

            for (YeeNotice notice : noticeList) {
                if (notice.getClassIds() != null) {
                    classIdSet.addAll(notice.getClassIds());
                }
                if (notice.getCourseId() > 0) {
                    courseIdSet.add(notice.getCourseId());
                }
            }

            // 批量查班级名
            Map<Integer, String> classIdToName = new HashMap<>();
            if (!classIdSet.isEmpty()) {
                String inClause = classIdSet.stream().map(id -> "?").collect(Collectors.joining(","));
                String classSql = "SELECT id, name FROM yee_course_class WHERE id IN (" + inClause + ") AND schoolId = ?";
                PreparedStatement classSt = connection.prepareStatement(classSql);
                int idx = 1;
                for (Integer id : classIdSet) {
                    classSt.setInt(idx++, id);
                }
                classSt.setInt(idx, schoolId);

                ResultSet classRs = classSt.executeQuery();
                while (classRs.next()) {
                    classIdToName.put(classRs.getInt("id"), classRs.getString("name"));
                }
                classRs.close();
                classSt.close();
            }

            // 批量查课程名
            Map<Long, String> courseIdToName = new HashMap<>();
            if (!courseIdSet.isEmpty()) {
                String inClause = courseIdSet.stream().map(id -> "?").collect(Collectors.joining(","));
                String courseSql = "SELECT id, name FROM yee_course WHERE id IN (" + inClause + ") AND schoolId = ?";
                PreparedStatement courseSt = connection.prepareStatement(courseSql);
                int idx = 1;
                for (Long id : courseIdSet) {
                    courseSt.setLong(idx++, id);
                }
                courseSt.setInt(idx, schoolId);

                ResultSet courseRs = courseSt.executeQuery();
                while (courseRs.next()) {
                    courseIdToName.put(courseRs.getLong("id"), courseRs.getString("name"));
                }
                courseRs.close();
                courseSt.close();
            }

            // 构建 VO 列表
            List<YeeNoticeVo> voList = new ArrayList<>();
            for (YeeNotice notice : noticeList) {
                YeeNoticeVo vo = new YeeNoticeVo();
                vo.setId(notice.getId());
                vo.setCourseId(notice.getCourseId());
                vo.setType(notice.getType());
                vo.setClassIds(notice.getClassIds());
                vo.setUserNumber(notice.getUserNumber());
                vo.setTitle(notice.getTitle());
                vo.setSummary(notice.getSummary());
                vo.setContent(notice.getContent());
                vo.setUserId(notice.getUserId());
                vo.setAddTime(notice.getAddTime());
                vo.setIsPush(notice.getIsPush());
                vo.setPushTime(notice.getPushTime());
                vo.setSysPush(notice.getSysPush());
                vo.setSchoolId(notice.getSchoolId());

                // 设置课程名称
                String courseName = courseIdToName.get(notice.getCourseId());
                vo.setCourseName(courseName != null ? courseName : "");

                // 设置班级名称（逗号分隔）
                StringBuilder classNames = new StringBuilder();
                if (notice.getClassIds() != null) {
                    for (Integer id : notice.getClassIds()) {
                        String name = classIdToName.get(id);
                        if (name != null) {
                            if (classNames.length() > 0) {
                                classNames.append(",");
                            }
                            classNames.append(name);
                        }
                    }
                }
                vo.setClassName(classNames.toString());

                voList.add(vo);
            }

            return Result.success(voList, (long) totalCount);

        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

//    @Override
//    public Result teacherSelect(int schoolId, String title, Integer type, Long courseId, int pageNum, int pageSize) {
//        Connection connection = null;
//        try {
//            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//            if (slSchool == null || slSchool.getAllow() == 0) {
//                return Result.error("学校不存在或未审核");
//            }
//
//            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//
//            // 构建基础查询
//            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_notice WHERE schoolId = ?");
//            List<Object> params = new ArrayList<>();
//            params.add(schoolId);
//
//            if (courseId != null && courseId > 0) {
//                sqlBuilder.append(" AND courseId = ?");
//                params.add(courseId);
//            }
//            if (type != null && type >= 0) {
//                sqlBuilder.append(" AND type = ?");
//                params.add(type);
//            }
//            if (title != null && !title.trim().isEmpty()) {
//                sqlBuilder.append(" AND title LIKE ?");
//                params.add("%" + title.trim() + "%");
//            }
//
//            // 查询总数
//            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS tmp";
//            PreparedStatement countSt = connection.prepareStatement(countSql);
//            for (int i = 0; i < params.size(); i++) {
//                countSt.setObject(i + 1, params.get(i));
//            }
//            ResultSet countRs = countSt.executeQuery();
//            int totalCount = 0;
//            if (countRs.next()) {
//                totalCount = countRs.getInt(1);
//            }
//            countRs.close();
//            countSt.close();
//
//            // 分页查询
//            sqlBuilder.append(" ORDER BY addTime DESC LIMIT ? OFFSET ?");
//            params.add(pageSize);
//            params.add((pageNum - 1) * pageSize);
//
//            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());
//            for (int i = 0; i < params.size(); i++) {
//                st.setObject(i + 1, params.get(i));
//            }
//
//            ResultSet rs = st.executeQuery();
//            List<YeeNotice> noticeList = new ArrayList<>();
//            while (rs.next()) {
//                YeeNotice notice = rsToYeeNotice(rs); // ✅ 此时 classIds 是 List<Integer>
//                noticeList.add(notice);
//            }
//            rs.close();
//            st.close();
//
//            // ====== 提取所有 classId 并去重 ======
//            Set<Integer> classIdSet = new HashSet<>();
//            for (YeeNotice notice : noticeList) {
//                if (notice.getClassIds() != null) {
//                    classIdSet.addAll(notice.getClassIds()); // ✅ 直接 addAll List<Integer>
//                }
//            }
//
//            // 批量查班级名
//            Map<Integer, String> classIdToName = new HashMap<>();
//            if (!classIdSet.isEmpty()) {
//                String inClause = classIdSet.stream().map(id -> "?").collect(Collectors.joining(","));
//                String classSql = "SELECT id, name FROM yee_classes WHERE id IN (" + inClause + ") AND schoolId = ?";
//                PreparedStatement classSt = connection.prepareStatement(classSql);
//                int idx = 1;
//                for (Integer id : classIdSet) {
//                    classSt.setInt(idx++, id);
//                }
//                classSt.setInt(idx, schoolId);
//
//                ResultSet classRs = classSt.executeQuery();
//                while (classRs.next()) {
//                    classIdToName.put(classRs.getInt("id"), classRs.getString("name"));
//                }
//                classRs.close();
//                classSt.close();
//            }
//
//            // 构建 VO 列表
//            List<YeeNoticeVo> voList = new ArrayList<>();
//            for (YeeNotice notice : noticeList) {
//                YeeNoticeVo vo = new YeeNoticeVo();
//                // 复制字段（建议用 BeanUtils，但这里手写）
//                vo.setId(notice.getId());
//                vo.setCourseId(notice.getCourseId());
//                vo.setType(notice.getType());
//                vo.setClassIds(notice.getClassIds());
//                vo.setUserNumber(notice.getUserNumber());
//                vo.setTitle(notice.getTitle());
//                vo.setSummary(notice.getSummary());
//                vo.setContent(notice.getContent());
//                vo.setUserId(notice.getUserId());
//                vo.setAddTime(notice.getAddTime());
//                vo.setIsPush(notice.getIsPush());
//                vo.setPushTime(notice.getPushTime());
//                vo.setSysPush(notice.getSysPush());
//                vo.setSchoolId(notice.getSchoolId());
//
//                // 填充 className
//                StringBuilder classNames = new StringBuilder();
//                if (notice.getClassIds() != null) {
//                    for (Integer id : notice.getClassIds()) {
//                        String name = classIdToName.get(id);
//                        if (name != null) {
//                            if (classNames.length() > 0) {
//                                classNames.append(",");
//                            }
//                            classNames.append(name);
//                        }
//                    }
//                }
//                vo.setClassName(classNames.toString());
//
//                voList.add(vo);
//            }
//
//            return Result.success(voList, (long) totalCount);
//
//        } catch (Exception e) {
//            return Result.error("查询失败：" + e.getMessage());
//        } finally {
//            if (connection != null) {
//                try {
//                    connection.close();
//                } catch (SQLException ignored) {}
//            }
//        }
//    }


    @Override
    public Result add(YeeNotice notice) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) notice.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // ===== 新增：根据 isPush 自动设置 pushTime =====
            if ( notice.getIsPush() == 1) {
                // 立即推送：设置推送时间为当前时间
                notice.setPushTime(new Timestamp(System.currentTimeMillis()));
            }
            // ============================================

            StringBuilder columns = new StringBuilder("INSERT INTO yee_notice (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();

            // 必填字段
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(notice.getSchoolId());

            columns.append("`courseId`, ");
            values.append("?, ");
            parameters.add(notice.getCourseId());

            // 动态字段处理
            if (notice.getType() >= 0) {
                columns.append("`type`, ");
                values.append("?, ");
                parameters.add(notice.getType());
            }

            // 处理 classIds: List<Integer> -> JSON String
            if (notice.getClassIds() != null && !notice.getClassIds().isEmpty()) {
                try {
                    String classIdsJson = objectMapper.writeValueAsString(notice.getClassIds());
                    columns.append("`classIds`, ");
                    values.append("?, ");
                    parameters.add(classIdsJson);
                } catch (JsonProcessingException e) {
                    return Result.error("班级ID列表格式错误");
                }
            }

            if (notice.getUserNumber() != null && !notice.getUserNumber().trim().isEmpty()) {
                columns.append("`userNumber`, ");
                values.append("?, ");
                parameters.add(notice.getUserNumber());
            }

            if (notice.getTitle() != null && !notice.getTitle().trim().isEmpty()) {
                columns.append("`title`, ");
                values.append("?, ");
                parameters.add(notice.getTitle());
            }

            if (notice.getSummary() != null && !notice.getSummary().trim().isEmpty()) {
                columns.append("`summary`, ");
                values.append("?, ");
                parameters.add(notice.getSummary());
            }

            if (notice.getContent() != null && !notice.getContent().trim().isEmpty()) {
                columns.append("`content`, ");
                values.append("?, ");
                parameters.add(notice.getContent());
            }

            if (notice.getUserId() > 0) {
                columns.append("`userId`, ");
                values.append("?, ");
                parameters.add(notice.getUserId());
            }

            if (notice.getIsPush() >= 0) {
                columns.append("`isPush`, ");
                values.append("?, ");
                parameters.add(notice.getIsPush());
            }

            // ✅ 现在 pushTime 已被自动设置（如果是立即推送）
            if (notice.getPushTime() != null) {
                columns.append("`pushTime`, ");
                values.append("?, ");
                parameters.add(notice.getPushTime());
            }

            if (notice.getSysPush() >= 0) {
                columns.append("`sysPush`, ");
                values.append("?, ");
                parameters.add(notice.getSysPush());
            }

            // addTime
            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(notice.getAddTime() != null ? notice.getAddTime() : new Timestamp(System.currentTimeMillis()));

            // 移除末尾逗号
            columns.setLength(columns.length() - 2);
            values.setLength(values.length() - 2);

            columns.append(") ");
            values.append(")");
            String sql = columns.toString() + values.toString();

            PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            setPreparedStatementParams(st, parameters);

            int rowsInserted = st.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = st.getGeneratedKeys();
                if (generatedKeys.next()) {
                    notice.setId(generatedKeys.getLong(1));
                }
                generatedKeys.close();
            }

            st.close();
            connection.close();

            return rowsInserted > 0 ? Result.success("添加成功") : Result.error("添加失败");

        } catch (Exception e) {
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @Override
    public Result update(YeeNotice notice) {
        if (notice.getId() <= 0) {
            return Result.error("ID不能为空");
        }

        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) notice.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            StringBuilder sql = new StringBuilder("UPDATE yee_notice SET ");
            List<Object> parameters = new ArrayList<>();

            if (notice.getCourseId() > 0) {
                sql.append("`courseId` = ?, ");
                parameters.add(notice.getCourseId());
            }

            if (notice.getType() >= 0) {
                sql.append("`type` = ?, ");
                parameters.add(notice.getType());
            }

            // classIds: List<Integer> -> JSON String
            if (notice.getClassIds() != null && !notice.getClassIds().isEmpty()) {
                try {
                    String classIdsJson = objectMapper.writeValueAsString(notice.getClassIds());
                    sql.append("`classIds` = ?, ");
                    parameters.add(classIdsJson);
                } catch (JsonProcessingException e) {
                    return Result.error("班级ID列表格式错误");
                }
            }

            if (notice.getUserNumber() != null && !notice.getUserNumber().trim().isEmpty()) {
                sql.append("`userNumber` = ?, ");
                parameters.add(notice.getUserNumber());
            }

            if (notice.getTitle() != null && !notice.getTitle().trim().isEmpty()) {
                sql.append("`title` = ?, ");
                parameters.add(notice.getTitle());
            }

            if (notice.getSummary() != null && !notice.getSummary().trim().isEmpty()) {
                sql.append("`summary` = ?, ");
                parameters.add(notice.getSummary());
            }

            if (notice.getContent() != null && !notice.getContent().trim().isEmpty()) {
                sql.append("`content` = ?, ");
                parameters.add(notice.getContent());
            }

            if (notice.getUserId() > 0) {
                sql.append("`userId` = ?, ");
                parameters.add(notice.getUserId());
            }

            if (notice.getIsPush() >= 0) {
                sql.append("`isPush` = ?, ");
                parameters.add(notice.getIsPush());
            }

            // 如果 isPush == 1，自动设置 pushTime
            if (notice.getIsPush() == 1) {
                Timestamp pt = notice.getPushTime() != null ? notice.getPushTime() : new Timestamp(System.currentTimeMillis());
                sql.append("`pushTime` = ?, ");
                parameters.add(pt);
            }

            if (notice.getSysPush() >= 0) {
                sql.append("`sysPush` = ?, ");
                parameters.add(notice.getSysPush());
            }

            if (notice.getAddTime() != null) {
                sql.append("`addTime` = ?, ");
                parameters.add(notice.getAddTime());
            }

            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }

            // 移除末尾逗号
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE id = ?");
            parameters.add(notice.getId());

            PreparedStatement st = connection.prepareStatement(sql.toString());
            setPreparedStatementParams(st, parameters);

            int rowsUpdated = st.executeUpdate();
            st.close();
            connection.close();

            return rowsUpdated > 0 ? Result.success("更新成功") : Result.error("更新失败：未找到匹配记录");

        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    // 辅助方法：统一设置 PreparedStatement 参数
    private void setPreparedStatementParams(PreparedStatement st, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param instanceof String) {
                st.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                st.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                st.setInt(i + 1, (Integer) param);
            } else if (param instanceof Timestamp) {
                st.setTimestamp(i + 1, (Timestamp) param);
            } else if (param == null) {
                st.setNull(i + 1, Types.VARCHAR); // 或根据字段类型调整
            }
        }
    }

    @Override
    public Result delete(int schoolId, long id) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "DELETE FROM yee_notice WHERE id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, id);
            
            int rowsDeleted = st.executeUpdate();
            st.close();
            connection.close();
            
            if (rowsDeleted > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：未找到匹配的记录");
            }
            
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    //根据id查询消息详情
    @Override
    public Result selectById(int schoolId, long noticeId) {
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        String sql = "SELECT * FROM yee_notice WHERE id = ?";

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setLong(1, noticeId);

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    YeeNotice yeeNotice = rsToYeeNotice(rs);
                    return Result.success(yeeNotice);
                } else {
                    return Result.error("未找到该消息");
                }
            }

        } catch (Exception e) {
            return Result.error("查询失败，请稍后重试",e.getMessage());
        }
    }
    //学生获取消息列表
    @Override
    public Result studentSelect(int schoolId, long studentId, int pageSize, int pageNum) {
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        int offset = (pageNum - 1) * pageSize;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            // 2. 先确认学生存在并获取学号（用于 type=3）
            String checkStudentSql = "SELECT number FROM yee_student WHERE id = ? AND schoolId = ?";
            String userNumber = null;
            try (PreparedStatement ps = conn.prepareStatement(checkStudentSql)) {
                ps.setLong(1, studentId);
                ps.setInt(2, schoolId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("学生不存在或不属于该校");
                    }
                    userNumber = rs.getString("number");
                }
            }

            // 3. 查询总数量（用于分页）
            String countSql = """
            SELECT COUNT(DISTINCT n.id)
            FROM yee_notice n
            LEFT JOIN yee_course_student cs 
                ON n.courseId = cs.courseId 
                AND n.schoolId = cs.schoolId 
                AND cs.studentId = ?
            CROSS JOIN (SELECT ? AS number) stu
            WHERE 
                n.schoolId = ?
                AND n.isPush = 1
                AND (
                    n.type = 1
                    OR (n.type = 2 AND cs.classId IS NOT NULL AND JSON_CONTAINS(n.classIds, CAST(cs.classId AS JSON), ' $ '))
                    OR (n.type = 3 AND n.userNumber = stu.number)
                )
            """;

            long total = 0;
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                countStmt.setLong(1, studentId);      // cs.studentId
                countStmt.setString(2, userNumber);   // stu.number
                countStmt.setInt(3, schoolId);        // n.schoolId
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }

            if (total == 0) {
                return Result.success(Collections.emptyList(), 0L);
            }

            // 4. 分页查询通知列表
            String noticeSql = """
            SELECT DISTINCT n.*
            FROM yee_notice n
            LEFT JOIN yee_course_student cs 
                ON n.courseId = cs.courseId 
                AND n.schoolId = cs.schoolId 
                AND cs.studentId = ?
            CROSS JOIN (SELECT ? AS number) stu
            WHERE 
                n.schoolId = ?
                AND n.isPush = 1
                AND (
                    n.type = 1
                    OR (n.type = 2 AND cs.classId IS NOT NULL AND JSON_CONTAINS(n.classIds, CAST(cs.classId AS JSON), ' $ '))
                    OR (n.type = 3 AND n.userNumber = stu.number)
                )
            ORDER BY n.addTime DESC
            LIMIT ? OFFSET ?
            """;

            List<YeeNotice> notices = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(noticeSql)) {
                stmt.setLong(1, studentId);
                stmt.setString(2, userNumber);
                stmt.setInt(3, schoolId);
                stmt.setInt(4, pageSize);
                stmt.setInt(5, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notices.add(rsToYeeNotice(rs));
                    }
                }
            }

            return Result.success(notices, total);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询消息失败，请稍后重试");
        }
    }
}
