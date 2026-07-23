package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeCategory;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeCategoryService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;

@Service
public class YeeCategoryServiceImpl implements YeeCategoryService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private Object rsToYeeCategory(ResultSet rs) throws SQLException {
        ArrayList<YeeCategory> yeeCategories = new ArrayList<>();
        while (rs.next()) {
            YeeCategory yeeCategory = new YeeCategory();
            yeeCategory.setId(rs.getLong("id"));
            yeeCategory.setName(rs.getString("name"));
            yeeCategory.setAllow(rs.getLong("allow"));
            yeeCategory.setSchoolId(rs.getLong("schoolId"));
            yeeCategory.setPid(rs.getLong("pid"));
            yeeCategory.setCode(rs.getString("code"));
            yeeCategories.add(yeeCategory);
        }
        return yeeCategories;
    }

//    @Override
//    public Result selectAll(int schoolId, int allow) throws Exception {
//        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            return Result.error("学校不存在或未审核");
//        }
//
//        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//        String sql = "SELECT * FROM yee_category";
//        String countSql = "SELECT COUNT(*) FROM yee_category";
//
//        PreparedStatement countSt = connection.prepareStatement(countSql);
//        PreparedStatement st = connection.prepareStatement(sql);
//
//        ResultSet rs = st.executeQuery();
//        ResultSet countRs = countSt.executeQuery();
//
//        int count = 0;
//        if (countRs.next()) {
//            count = countRs.getInt(1);
//        }
//
//        Object categories = rsToYeeCategory(rs);
//
//        rs.close();
//        countRs.close();
//        st.close();
//        countSt.close();
//        connection.close();
//
//        return Result.success(categories, (long) count);
//    }
        @Override
        public Result selectAll(int schoolId, Integer allow) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

        StringBuilder sql = new StringBuilder("SELECT * FROM yee_category");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM yee_category");

        boolean hasCondition = allow != null;

        if (hasCondition) {
            sql.append(" WHERE allow = ?");
            countSql.append(" WHERE allow = ?");
        }

        try (PreparedStatement st = connection.prepareStatement(sql.toString());
             PreparedStatement countSt = connection.prepareStatement(countSql.toString())) {

            if (hasCondition) {
                st.setInt(1, allow);
                countSt.setInt(1, allow);
            }

            try (ResultSet rs = st.executeQuery();
                 ResultSet countRs = countSt.executeQuery()) {

                int count = countRs.next() ? countRs.getInt(1) : 0;
                Object categories = rsToYeeCategory(rs);

                return Result.success(categories, (long) count);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Override
    public Result add(YeeCategory yeeCategory) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById((int) yeeCategory.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        
        StringBuilder columns = new StringBuilder("INSERT INTO yee_category (");
        StringBuilder values = new StringBuilder("VALUES (");
        ArrayList<Object> parameters = new ArrayList<>();
        
        // 必填字段：schoolId
        columns.append("`schoolId`, ");
        values.append("?, ");
        parameters.add(yeeCategory.getSchoolId());
        
        // 动态添加可选字段
        if (yeeCategory.getName() != null && !yeeCategory.getName().trim().isEmpty()) {
            columns.append("`name`, ");
            values.append("?, ");
            parameters.add(yeeCategory.getName());
        }
        
        if (yeeCategory.getAllow() >= 0) {
            columns.append("`allow`, ");
            values.append("?, ");
            parameters.add(yeeCategory.getAllow());
        }
        
        if (yeeCategory.getPid() >= 0) {
            columns.append("`pid`, ");
            values.append("?, ");
            parameters.add(yeeCategory.getPid());
        }
        
        if (yeeCategory.getCode() != null && !yeeCategory.getCode().trim().isEmpty()) {
            columns.append("`code`, ");
            values.append("?, ");
            parameters.add(yeeCategory.getCode());
        }
        
        // 删除最后的逗号和空格
        columns.delete(columns.length() - 2, columns.length());
        values.delete(values.length() - 2, values.length());
        
        // 构建完整SQL
        columns.append(") ");
        values.append(")");
        String sql = columns.toString() + values.toString();
        
        PreparedStatement st = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param instanceof String) {
                st.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                st.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                st.setInt(i + 1, (Integer) param);
            }
        }
        
        int rowsInserted = st.executeUpdate();
        st.close();
        connection.close();
        
        if (rowsInserted > 0) {
            return Result.success("添加成功");
        } else {
            return Result.error("添加失败");
        }
    }

    @Override
    public Result update(YeeCategory yeeCategory) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById((int) yeeCategory.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        
        StringBuilder sql = new StringBuilder("UPDATE yee_category SET ");
        ArrayList<Object> parameters = new ArrayList<>();
        
        // 动态添加更新字段
        if (yeeCategory.getName() != null && !yeeCategory.getName().trim().isEmpty()) {
            sql.append("`name` = ?, ");
            parameters.add(yeeCategory.getName());
        }
        
        if (yeeCategory.getAllow() >= 0) {
            sql.append("`allow` = ?, ");
            parameters.add(yeeCategory.getAllow());
        }
        
        if (yeeCategory.getPid() >= 0) {
            sql.append("`pid` = ?, ");
            parameters.add(yeeCategory.getPid());
        }
        
        if (yeeCategory.getCode() != null && !yeeCategory.getCode().trim().isEmpty()) {
            sql.append("`code` = ?, ");
            parameters.add(yeeCategory.getCode());
        }
        
        // 检查是否有可更新的字段
        if (parameters.isEmpty()) {
            connection.close();
            return Result.error("没有可更新的字段");
        }
        
        // 删除最后的逗号和空格
        sql.delete(sql.length() - 2, sql.length());
        
        // 添加WHERE条件
        sql.append(" WHERE id = ?");
        parameters.add(yeeCategory.getId());
        
        PreparedStatement st = connection.prepareStatement(sql.toString());
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param instanceof String) {
                st.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                st.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                st.setInt(i + 1, (Integer) param);
            }
        }
        
        int rowsUpdated = st.executeUpdate();
        st.close();
        connection.close();
        
        if (rowsUpdated > 0) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败：未找到匹配的记录");
        }
    }

    @Override
    public Result delete(int schoolId, int id) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        String sql = "DELETE FROM yee_category WHERE id = ?";
        PreparedStatement st = connection.prepareStatement(sql);
        st.setInt(1, id);
        
        int rowsDeleted = st.executeUpdate();
        st.close();
        connection.close();
        
        if (rowsDeleted > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败：未找到匹配的记录");
        }
    }
}
