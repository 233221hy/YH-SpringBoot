package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeNodeFiles;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeNodeFilesService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class YeeNodeFilesServiceImpl implements YeeNodeFilesService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeNodeFiles rsToYeeNodeFiles(ResultSet rs) throws SQLException {
        YeeNodeFiles yeeNodeFiles = new YeeNodeFiles();
        yeeNodeFiles.setId(rs.getLong("id"));
        yeeNodeFiles.setNodeId(rs.getLong("nodeId"));
        yeeNodeFiles.setCourseId(rs.getLong("courseId"));
        yeeNodeFiles.setName(rs.getString("name"));
        yeeNodeFiles.setUploadPath(rs.getString("uploadPath"));
        yeeNodeFiles.setTimeView(rs.getLong("timeView"));
        yeeNodeFiles.setCreateUserId(rs.getLong("createUserId"));
        yeeNodeFiles.setAddTime(rs.getTimestamp("addTime"));
        yeeNodeFiles.setFileName(rs.getString("fileName"));
        yeeNodeFiles.setSchoolId(rs.getLong("schoolId"));
        return yeeNodeFiles;
    }

    @Override
    public Result selectByNodeId(Integer schoolId, long nodeId) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "SELECT * FROM yee_node_files WHERE nodeId = ?";
            String countSql = "SELECT COUNT(*) FROM yee_node_files WHERE nodeId = ?";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, nodeId);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, nodeId);
            ResultSet rs = st.executeQuery();
            
            List<YeeNodeFiles> nodeFiles = new ArrayList<>();
            while (rs.next()) {
                YeeNodeFiles file = rsToYeeNodeFiles(rs);
                nodeFiles.add(file);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(nodeFiles, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeeNodeFiles nodeFiles) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) nodeFiles.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder columns = new StringBuilder("INSERT INTO yee_node_files (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();
            
            // 必填字段：schoolId, nodeId
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(nodeFiles.getSchoolId());
            
            columns.append("`nodeId`, ");
            values.append("?, ");
            parameters.add(nodeFiles.getNodeId());
            
            // 动态添加可选字段
            if (nodeFiles.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                parameters.add(nodeFiles.getCourseId());
            }
            
            if (nodeFiles.getName() != null && !nodeFiles.getName().trim().isEmpty()) {
                columns.append("`name`, ");
                values.append("?, ");
                parameters.add(nodeFiles.getName());
            }
            
            if (nodeFiles.getUploadPath() != null && !nodeFiles.getUploadPath().trim().isEmpty()) {
                columns.append("`uploadPath`, ");
                values.append("?, ");
                parameters.add(nodeFiles.getUploadPath());
            }
            
            if (nodeFiles.getTimeView() >= 0) {
                columns.append("`timeView`, ");
                values.append("?, ");
                parameters.add(nodeFiles.getTimeView());
            }
            
            if (nodeFiles.getCreateUserId() > 0) {
                columns.append("`createUserId`, ");
                values.append("?, ");
                parameters.add(nodeFiles.getCreateUserId());
            }
            
            if (nodeFiles.getFileName() != null && !nodeFiles.getFileName().trim().isEmpty()) {
                columns.append("`fileName`, ");
                values.append("?, ");
                parameters.add(nodeFiles.getFileName());
            }
            
            // 自动设置添加时间
            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(nodeFiles.getAddTime() != null ? nodeFiles.getAddTime() : new Timestamp(System.currentTimeMillis()));
            
            // 删除最后的逗号和空格
            columns.delete(columns.length() - 2, columns.length());
            values.delete(values.length() - 2, values.length());
            
            // 构建完整SQL
            columns.append(") ");
            values.append(")");
            String sql = columns.toString() + values.toString();
            
            PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
                }
            }
            
            int rowsInserted = st.executeUpdate();
            if (rowsInserted > 0) {
                // 获取生成的ID
                ResultSet generatedKeys = st.getGeneratedKeys();
                if (generatedKeys.next()) {
                    nodeFiles.setId(generatedKeys.getLong(1));
                }
                generatedKeys.close();
                
                st.close();
                connection.close();
                return Result.success("添加成功");
            } else {
                st.close();
                connection.close();
                return Result.error("添加失败");
            }
            
        } catch (Exception e) {
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @Override
    public Result update(YeeNodeFiles nodeFiles) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) nodeFiles.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder sql = new StringBuilder("UPDATE yee_node_files SET ");
            List<Object> parameters = new ArrayList<>();
            
            // 动态添加更新字段
            if (nodeFiles.getNodeId() > 0) {
                sql.append("`nodeId` = ?, ");
                parameters.add(nodeFiles.getNodeId());
            }
            
            if (nodeFiles.getCourseId() > 0) {
                sql.append("`courseId` = ?, ");
                parameters.add(nodeFiles.getCourseId());
            }
            
            if (nodeFiles.getName() != null && !nodeFiles.getName().trim().isEmpty()) {
                sql.append("`name` = ?, ");
                parameters.add(nodeFiles.getName());
            }
            
            if (nodeFiles.getUploadPath() != null && !nodeFiles.getUploadPath().trim().isEmpty()) {
                sql.append("`uploadPath` = ?, ");
                parameters.add(nodeFiles.getUploadPath());
            }
            
            if (nodeFiles.getTimeView() >= 0) {
                sql.append("`timeView` = ?, ");
                parameters.add(nodeFiles.getTimeView());
            }
            
            if (nodeFiles.getCreateUserId() > 0) {
                sql.append("`createUserId` = ?, ");
                parameters.add(nodeFiles.getCreateUserId());
            }
            
            if (nodeFiles.getFileName() != null && !nodeFiles.getFileName().trim().isEmpty()) {
                sql.append("`fileName` = ?, ");
                parameters.add(nodeFiles.getFileName());
            }
            
            if (nodeFiles.getAddTime() != null) {
                sql.append("`addTime` = ?, ");
                parameters.add(nodeFiles.getAddTime());
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
            parameters.add(nodeFiles.getId());
            
            PreparedStatement st = connection.prepareStatement(sql.toString());
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
            
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result delete(Integer schoolId, long id) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "DELETE FROM yee_node_files WHERE id = ?";
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
}
