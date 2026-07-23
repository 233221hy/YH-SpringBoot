package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeSignInRecord;
import cn.xfywz.guozespring.entity.dto.YeeSignInRecordQuery;
import cn.xfywz.guozespring.excel.TimestampConverter;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.YeeSignInRecordService;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import com.alibaba.excel.EasyExcel;
import cn.xfywz.guozespring.excel.ResponseExportUtil;
import cn.xfywz.guozespring.excel.ExcelExportStyles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YeeSignInRecordServiceImpl implements YeeSignInRecordService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Override
    public void add(YeeSignInRecord param) {
        SlSchool slSchool = slSchoolMapper.selectById((int) param.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            // 学校不存在或未审核，抛出异常避免控制器误返回成功
            throw new RuntimeException("学校不存在或未审核");
        }
        // 根据yee_sign_in表的结束时间判断state：signTime <= endTime 为正常(1)，否则迟到(2)
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            Timestamp signTime = param.getSignTime() != null ? param.getSignTime() : new Timestamp(System.currentTimeMillis());
            Timestamp endTime = null;
            // 获取签到结束时间
            try (PreparedStatement st = conn.prepareStatement("SELECT endTime FROM yee_sign_in WHERE id = ?")) {
                st.setLong(1, param.getSignId());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        endTime = rs.getTimestamp(1);
                    }
                }
            }
            long state = 1;//正常
            if (endTime != null && signTime.after(endTime)) {
                state = 2;//迟到
            }
            param.setState(state);

            String sql = "INSERT INTO yee_sign_in_record (signId, userId, signTime, courseId, classId, schoolId, state) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, param.getSignId());
                ps.setLong(2, param.getUserId());
                ps.setTimestamp(3, signTime);
                ps.setLong(4, param.getCourseId());
                ps.setLong(5, param.getClassId());
                ps.setLong(6, param.getSchoolId());
                ps.setLong(7, state);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("签到记录插入失败: " + e.getMessage(), e);
        }
    }


    @Override
    public Result selectById(int schoolId, long id) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            // 学校不存在或未审核，抛出异常避免控制器误返回成功
            throw new RuntimeException("学校不存在或未审核");
        }
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM yee_sign_in_record WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    YeeSignInRecord yeeSignInRecord = new YeeSignInRecord();
                    yeeSignInRecord.setId(rs.getLong(1));
                    yeeSignInRecord.setSignId(rs.getLong(2));
                    yeeSignInRecord.setUserId(rs.getLong(3));
                    yeeSignInRecord.setSignTime(rs.getTimestamp(4));
                    yeeSignInRecord.setCourseId(rs.getLong(5));
                    yeeSignInRecord.setClassId(rs.getLong(6));
                    yeeSignInRecord.setSchoolId(rs.getLong(7));
                    yeeSignInRecord.setState(rs.getLong(8));
                    rs.close();
                    return Result.success(yeeSignInRecord);
                }
                return Result.error("未找到记录");
            } catch (Exception e) {
                throw new RuntimeException("查询失败: " + e.getMessage(), e);
            } finally {
                ps.close();
                connection.close();
            }
        }
    }

    @Override
    public Result list(YeeSignInRecordQuery param) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            // 学校不存在或未审核，抛出异常避免控制器误返回成功
            throw new RuntimeException("学校不存在或未审核");
        }
        int pageNum = (param.getPageNum() == null || param.getPageNum() < 1) ? 1 : param.getPageNum();
        int pageSize = (param.getPageSize() == null || param.getPageSize() < 1) ? 10 : param.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        //基础from
        String baseFrom = " FROM yee_sign_in_record AS sir " +
                " LEFT JOIN yee_student AS st ON st.id = sir.userId ";

        //where条件
        StringBuilder where = new StringBuilder(" WHERE sir.schoolId = ? AND sir.signId = ?");
        List<Object> params = new ArrayList<>();
        params.add(param.getSchoolId());
        params.add(param.getSignId());
        if (param.getClassId() != null && param.getClassId() > 0) {
            where.append(" AND sir.classId = ?");
            params.add(param.getClassId());
        }
        if (param.getState() != null && param.getState() > 0) {
            where.append(" AND sir.state = ?");
            params.add(param.getState());
        }
        if (param.getGender() != null && param.getGender() > 0){
            where.append(" AND st.gender = ?");
            params.add(param.getGender());
        }
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            where.append(" AND (st.name LIKE ? OR st.number LIKE ?)");
            String like = "%" + param.getKeyword().trim() + "%";
            params.add(like); params.add(like);
        }

        // sql语句
        String sql = "SELECT sir.id, sir.signId, sir.userId, sir.signTime, sir.courseId, sir.classId, sir.schoolId, sir.state, st.name AS studentName, st.number AS studentNumber " +
                baseFrom + where + " ORDER BY sir.id DESC LIMIT ?, ?";

        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            for (Object p : params) ps.setObject(idx++, p);
            ps.setInt(idx++, offset);
            ps.setInt(idx++, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("signId", rs.getLong("signId"));
                    row.put("userId", rs.getLong("userId"));
                    row.put("signTime", rs.getTimestamp("signTime"));
                    row.put("courseId", rs.getLong("courseId"));
                    row.put("classId", rs.getLong("classId"));
                    row.put("schoolId", rs.getLong("schoolId"));
                    row.put("state", rs.getLong("state"));
                    row.put("studentName", rs.getString("studentName"));
                    row.put("studentNumber", rs.getString("studentNumber"));
                    list.add(row);
                }
                return Result.success(list, (long) list.size());
            }
        }
        catch (Exception e) {
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(YeeSignInRecord param) {
        SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            // 学校不存在或未审核，抛出异常避免控制器误返回成功
            throw new RuntimeException("学校不存在或未审核");
        }

        class SqlBuilder {
            BuiltSql buildUpdateById(YeeSignInRecord param) {
                LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
                if (param.getSignTime() != null) fields.put("signTime", param.getSignTime());
                if (param.getState() > 0) fields.put("state", param.getState());
                if (fields.isEmpty()) return null;
                String setClause = fields.keySet().stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(", "));
                List<Object> params = new ArrayList<>(fields.values());
                params.add(param.getId());
                params.add(param.getSchoolId());
                String sql = "UPDATE yee_sign_in_record SET " + setClause + " WHERE id = ? AND schoolId = ?";
                return BuiltSql.of(sql, params);
            }
            BuiltSql buildUpdateByKeys(YeeSignInRecord param) {
                LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
                if (param.getSignTime() != null) fields.put("signTime", param.getSignTime());
                if (param.getState() > 0) fields.put("state", param.getState());
                if (fields.isEmpty()) return null;
                String setClause = fields.keySet().stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(", "));
                List<Object> params = new ArrayList<>(fields.values());
                params.add(param.getSchoolId());
                params.add(param.getSignId());
                params.add(param.getUserId());
                String sql = "UPDATE yee_sign_in_record SET " + setClause + " WHERE schoolId = ? AND signId = ? AND userId = ?";
                return BuiltSql.of(sql, params);
            }
        }
        class DbExecutor {
            int execute(SlSchool school, BuiltSql built) throws Exception {
                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
                     PreparedStatement st = connection.prepareStatement(built.sql())) {
                    for (int i = 0; i < built.params().size(); i++) {
                        st.setObject(i + 1, built.params().get(i));
                    }
                    return st.executeUpdate();
                }
            }
        }
        SqlBuilder builder = new SqlBuilder();
        DbExecutor executor = new DbExecutor();
        try {
            BuiltSql built = null;
            if (param.getId() > 0) built = builder.buildUpdateById(param);
            else if (param.getSignId() > 0 && param.getUserId() > 0) built = builder.buildUpdateByKeys(param);
            if (built == null) throw new RuntimeException("缺少可更新字段或定位条件");
            int result = executor.execute(slSchool, built);
            if (result <= 0) throw new RuntimeException("未匹配记录");
        } catch (Exception e) {
            throw new RuntimeException("更新失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void exportData(YeeSignInRecordQuery queryDTO, HttpServletResponse response) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(queryDTO.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new RuntimeException("学校不存在或未审核");
        }
        String baseFrom = """
                 FROM yee_sign_in_record sir
                LEFT JOIN yee_student st ON st.id = sir.userId
                LEFT JOIN yee_course_class cl ON cl.id = sir.classId
                """;
        StringBuilder where = new StringBuilder(" WHERE sir.schoolId = ? AND sir.signId = ?");
        List<Object> params = new ArrayList<>();
        params.add(queryDTO.getSchoolId());
        params.add(queryDTO.getSignId());
        if (queryDTO.getClassId() != null && queryDTO.getClassId() > 0) { where.append(" AND sir.classId = ?"); params.add(queryDTO.getClassId()); }
        if (queryDTO.getState() != null && queryDTO.getState() > 0) { where.append(" AND sir.state = ?"); params.add(queryDTO.getState()); }
        if (queryDTO.getGender() != null && queryDTO.getGender() > 0) { where.append(" AND st.gender = ?"); params.add(queryDTO.getGender()); }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
            where.append(" AND (st.name LIKE ? OR st.number LIKE ?)");
            String like = "%" + queryDTO.getKeyword().trim() + "%";
            params.add(like); params.add(like);
        }
        String sql = "SELECT st.number, st.name, st.gender, sir.signTime, sir.state, cl.name AS className" + baseFrom + where + " ORDER BY sir.id DESC";
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                row.add(rs.getString("number"));
                row.add(rs.getString("name"));
                row.add(rs.getString("gender"));
                row.add(rs.getTimestamp("signTime"));
                Long state = rs.getLong("state");
                String stateText = state == 1 ? "正常" : (state == 2 ? "迟到" : String.valueOf(state));
                row.add(stateText);//签到状态
                row.add(rs.getString("className"));
                rows.add(row);
            }
            String filename = "签到记录_" + System.currentTimeMillis();
            ResponseExportUtil.setExcelRespProp(response, filename);
            List<List<String>> head = new ArrayList<>();
            String title = "签到记录（" + new SimpleDateFormat("yyyy年MM月dd日").format(new Date()) + " 导出）";


            String[] headers = new String[]{"学号","姓名","性别","签到时间","状态","班级"};
            for (String h : headers) head.add(Arrays.asList(title, h));
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .registerConverter(new TimestampConverter())
                    .registerWriteHandler(ExcelExportStyles.defaultTitleRow(headers.length))
//                    .registerWriteHandler(ExcelExportStyles.createFreezeAndWidthHandler(new int[]{14,14,10,22,10,16}, 2))
//                    .registerWriteHandler(ExcelExportStyles.textColumns(new int[]{0}))
                    .sheet("签到记录")
                    .doWrite(rows);
        }
    }


}
