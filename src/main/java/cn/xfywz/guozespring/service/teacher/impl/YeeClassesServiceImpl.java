package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeClasses;
import cn.xfywz.guozespring.entity.dto.YeeClassesQueryParam;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeClassesService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class YeeClassesServiceImpl implements YeeClassesService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeClasses mapClass(ResultSet rs) throws Exception {
        YeeClasses c = new YeeClasses();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setCollegeId(rs.getInt("collegeId"));
        c.setAllow(rs.getInt("allow"));
        c.setAddTime(rs.getTimestamp("addTime"));
        c.setSchoolId(rs.getInt("schoolId"));
        try { c.setAddDate(rs.getDate("addDate")); } catch (SQLException ignored) {}
        try { c.setEntryYear(rs.getInt("entryYear")); } catch (SQLException ignored) {}
        try { c.setCount(rs.getInt("stuCount")); } catch (SQLException ignored) {}
        return c;
    }

    @Override
    public Result selectAll(int schoolId, int pageNum, int pageSize) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        int offset = (pageNum - 1) * pageSize;
        String base = " FROM yee_classes yc ";
        String countSql = "SELECT COUNT(*)" + base;
        String sql = "SELECT yc.*, (SELECT COUNT(*) FROM yee_student ys WHERE ys.classId = yc.id) AS stuCount" + base + " ORDER BY yc.addTime DESC LIMIT ? OFFSET ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement countPs = conn.prepareStatement(countSql);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet countRs = countPs.executeQuery();
            long total = 0;
            if (countRs.next()) total = countRs.getLong(1);
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            List<YeeClasses> list = new ArrayList<>();
            while (rs.next()) list.add(mapClass(rs));
            rs.close();
            countRs.close();
            return Result.success(list, total);
        }
    }

    @Override
    public Result selectById(int schoolId, long id) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        String sql = "SELECT yc.*, (SELECT COUNT(*) FROM yee_student ys WHERE ys.classId = yc.id) AS stuCount FROM yee_classes yc WHERE yc.id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                YeeClasses c = mapClass(rs);
                rs.close();
                return Result.success(c);
            }
            return Result.error("未找到记录");
        }
    }

    @Override
    public void add(YeeClasses c) throws Exception {
        if (c == null || c.getSchoolId() == null || c.getSchoolId() <= 0) {
            throw new RuntimeException("schoolId必填");
        }
        SlSchool slSchool = slSchoolMapper.selectById(c.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new RuntimeException("学校不存在或未审核");
        }
        StringBuilder cols = new StringBuilder("INSERT INTO yee_classes (");
        StringBuilder vals = new StringBuilder("VALUES (");
        List<Object> params = new ArrayList<>();
        cols.append("addTime, "); vals.append("?, "); params.add(new Timestamp(System.currentTimeMillis()));
        cols.append("schoolId, "); vals.append("?, "); params.add(c.getSchoolId());
        if (c.getName() != null){ cols.append("name, "); vals.append("?, "); params.add(c.getName()); }
        if (c.getCollegeId() != null && c.getCollegeId() > 0){ cols.append("collegeId, "); vals.append("?, "); params.add(c.getCollegeId()); }
        if (c.getAllow() != null){ cols.append("allow, "); vals.append("?, "); params.add(c.getAllow()); }
        if (c.getEntryYear() != null && c.getEntryYear() > 0){ cols.append("entryYear, "); vals.append("?, "); params.add(c.getEntryYear()); }
        if (cols.toString().endsWith(", ")) cols.setLength(cols.length()-2);
        if (vals.toString().endsWith(", ")) vals.setLength(vals.length()-2);
        cols.append(") "); vals.append(")");
        String sql = cols.toString() + vals;
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i+1, params.get(i));
            int n = ps.executeUpdate();
            if (n == 0) throw new RuntimeException("添加失败");
        }
    }

    @Override
    public void update(YeeClasses c) throws Exception {
        if (c == null || c.getId() == null || c.getId() <= 0 || c.getSchoolId() == null || c.getSchoolId() <= 0) {
            throw new RuntimeException("id与schoolId必填");
        }
        SlSchool slSchool = slSchoolMapper.selectById(c.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new RuntimeException("学校不存在或未审核");
        }
        StringBuilder sql = new StringBuilder("UPDATE yee_classes SET ");
        List<Object> params = new ArrayList<>();
        if (c.getName() != null){ sql.append("name = ?, "); params.add(c.getName()); }
        if (c.getCollegeId() != null && c.getCollegeId() > 0){ sql.append("collegeId = ?, "); params.add(c.getCollegeId()); }
        if (c.getAllow() != null){ sql.append("allow = ?, "); params.add(c.getAllow()); }
        if (c.getEntryYear() != null && c.getEntryYear() > 0){ sql.append("entryYear = ?, "); params.add(c.getEntryYear()); }
        if (sql.toString().endsWith(", ")) sql.setLength(sql.length()-2);
        sql.append(" WHERE id = ?");
        params.add(c.getId());
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i+1, params.get(i));
            int n = ps.executeUpdate();
            if (n == 0) throw new RuntimeException("更新失败");
        }
    }

    @Override
    public void delete(Long id, int schoolId) throws Exception {
        if (id == null || id <= 0 || schoolId <= 0) throw new RuntimeException("参数错误");
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) throw new RuntimeException("学校不存在或未审核");
        String sql = "DELETE FROM yee_classes WHERE id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Result searchByCondition(YeeClassesQueryParam param) throws Exception {
        if (param == null || param.getSchoolId() == null || param.getSchoolId() <= 0) {
            return Result.error("schoolId必填");
        }
        SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        StringBuilder base = new StringBuilder(" FROM yee_classes yc");
        StringBuilder where = new StringBuilder();
        List<Object> whereParams = new ArrayList<>();
        if (param.getName() != null && !param.getName().trim().isEmpty()) {
            where.append(where.length() == 0 ? " WHERE" : " AND");
            where.append(" yc.name LIKE ?");
            whereParams.add("%" + param.getName().trim() + "%");
        }
        if (param.getCollegeId() != null && param.getCollegeId() > 0) {
            where.append(where.length() == 0 ? " WHERE" : " AND");
            where.append(" yc.collegeId = ?");
            whereParams.add(param.getCollegeId());
        }
        if (param.getAllow() != null) {
            where.append(where.length() == 0 ? " WHERE" : " AND");
            where.append(" yc.allow = ?");
            whereParams.add(param.getAllow());
        }
        int pageNum = param.getPageNum() == null || param.getPageNum() < 1 ? 1 : param.getPageNum();
        int pageSize = param.getPageSize() == null || param.getPageSize() < 1 ? 10 : param.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        String countSql = "SELECT COUNT(*)" + base + where;
        String dataSql = "SELECT yc.*, (SELECT COUNT(*) FROM yee_student ys WHERE ys.classId = yc.id) AS stuCount" + base + where + " ORDER BY yc.addTime DESC LIMIT ? OFFSET ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement countPs = conn.prepareStatement(countSql);
             PreparedStatement dataPs = conn.prepareStatement(dataSql)) {
            for (int i = 0; i < whereParams.size(); i++) countPs.setObject(i+1, whereParams.get(i));
            ResultSet countRs = countPs.executeQuery();
            long total = 0;
            if (countRs.next()) total = countRs.getLong(1);
            for (int i = 0; i < whereParams.size(); i++) dataPs.setObject(i+1, whereParams.get(i));
            dataPs.setInt(whereParams.size()+1, pageSize);
            dataPs.setInt(whereParams.size()+2, offset);
            ResultSet rs = dataPs.executeQuery();
            List<YeeClasses> list = new ArrayList<>();
            while (rs.next()) list.add(mapClass(rs));
            rs.close();
            countRs.close();
            return Result.success(list, total);
        }
    }

    @Override
    public void lock(Long id, int schoolId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) throw new RuntimeException("学校不存在或未审核");
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        try {
            // 先查询当前用户的锁定状态
            String selectSql = "SELECT allow FROM yee_classes WHERE id = ?";
            PreparedStatement selectSt = connection.prepareStatement(selectSql);
            selectSt.setLong(1, id);
            ResultSet rs = selectSt.executeQuery();

            if (rs.next()) {
                int currentLockStatus = rs.getInt("allow");
                // 设置相反的锁定状态
                int newLockStatus = (currentLockStatus == 0) ? 1 : 0;

                // 更新锁定状态
                String updateSql = "UPDATE yee_classes SET allow = ? WHERE id = ?";
                PreparedStatement updateSt = connection.prepareStatement(updateSql);
                updateSt.setInt(1, newLockStatus);
                updateSt.setLong(2, id);

                int rowsUpdated = updateSt.executeUpdate();
                updateSt.close();

                if (rowsUpdated > 0) {
                    // 根据新状态返回相应的成功消息
                    if (newLockStatus == 1) {
                        Result.success("禁用成功");
                    } else {
                        Result.success("启用成功");
                    }
                } else {
                    Result.error("操作失败：未找到匹配的记录");
                }
            } else {
                Result.error("用户不存在");
            }

            rs.close();
            selectSt.close();
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean hasClassesByCollegeId(int schoolId, int collegeId) {
        Connection connection = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                Result.error("学校不存在或未审核");
            }
            // 构建SQL查询
            String sql = """
            SELECT COUNT(*) as classCount
            FROM yee_classes
            WHERE collegeId = ?
            AND schoolId = ?
            """;

            // 获取数据库连接并执行查询
            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            pst = connection.prepareStatement(sql);
            pst.setInt(1, collegeId);
            pst.setInt(2, schoolId);

            rs = pst.executeQuery();

            // 4. 解析结果
            if (rs.next()) {
                int classCount = rs.getInt("classCount");
                return classCount > 0; // 如果班级数量大于0，返回true
            }

            return false;

        }
        catch (Exception e) {
            return false;
        } finally {
            // 确保资源关闭
            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
                // 记录日志
            }
            try {
                if (pst != null) pst.close();
            } catch (Exception e) {
                // 记录日志
            }
            try {
                if (connection != null) connection.close();
            } catch (Exception e) {
                // 记录日志
            }
        }

    }
}
