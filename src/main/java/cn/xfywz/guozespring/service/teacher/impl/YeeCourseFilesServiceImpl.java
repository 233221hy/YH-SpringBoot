package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseFiles;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeCourseFilesService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class YeeCourseFilesServiceImpl implements YeeCourseFilesService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeCourseFiles rsToYeeCourseFiles(ResultSet rs) throws SQLException {
        YeeCourseFiles courseFiles = new YeeCourseFiles();
        courseFiles.setId(rs.getLong("id"));
        courseFiles.setCourseId(rs.getLong("courseId"));
        courseFiles.setName(rs.getString("name"));
        courseFiles.setUploadPath(rs.getString("uploadPath"));
        courseFiles.setTimeView(rs.getLong("timeView"));
        courseFiles.setCreateUserId(rs.getLong("createUserId"));
        courseFiles.setAddTime(rs.getTimestamp("addTime"));
        courseFiles.setFileName(rs.getString("fileName"));
        courseFiles.setSchoolId(rs.getLong("schoolId"));
        return courseFiles;
    }
    @Override
    public Result list(int pageSize, int pageNum, int schoolId, long courseId) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            int offset = (pageNum - 1) * pageSize;
            String sql = "SELECT * FROM yee_course_files WHERE courseId = ? ORDER BY addTime DESC LIMIT ? OFFSET ?";
            String countSql = "SELECT COUNT(*) FROM yee_course_files WHERE courseId = ?";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setInt(2, pageSize);
            st.setInt(3, offset);
            ResultSet rs = st.executeQuery();
            
            List<YeeCourseFiles> courseFilesList = new ArrayList<>();
            while (rs.next()) {
                YeeCourseFiles courseFiles = rsToYeeCourseFiles(rs);
                courseFilesList.add(courseFiles);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(courseFilesList, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeeCourseFiles yeeCourseFiles) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeCourseFiles.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder columns = new StringBuilder("INSERT INTO yee_course_files (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();
            
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(yeeCourseFiles.getSchoolId());
            
            if (yeeCourseFiles.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                parameters.add(yeeCourseFiles.getCourseId());
            }
            
            if (yeeCourseFiles.getName() != null && !yeeCourseFiles.getName().trim().isEmpty()) {
                columns.append("`name`, ");
                values.append("?, ");
                parameters.add(yeeCourseFiles.getName());
            }
            
            if (yeeCourseFiles.getUploadPath() != null && !yeeCourseFiles.getUploadPath().trim().isEmpty()) {
                columns.append("`uploadPath`, ");
                values.append("?, ");
                parameters.add(yeeCourseFiles.getUploadPath());
            }
            
            if (yeeCourseFiles.getFileName() != null && !yeeCourseFiles.getFileName().trim().isEmpty()) {
                columns.append("`fileName`, ");
                values.append("?, ");
                parameters.add(yeeCourseFiles.getFileName());
            }
            
            if (yeeCourseFiles.getTimeView() >= 0) {
                columns.append("`timeView`, ");
                values.append("?, ");
                parameters.add(yeeCourseFiles.getTimeView());
            }
            
            if (yeeCourseFiles.getCreateUserId() > 0) {
                columns.append("`createUserId`, ");
                values.append("?, ");
                parameters.add(yeeCourseFiles.getCreateUserId());
            }
            
            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(yeeCourseFiles.getAddTime() != null ? yeeCourseFiles.getAddTime() : new Timestamp(System.currentTimeMillis()));
            
            columns.delete(columns.length() - 2, columns.length());
            values.delete(values.length() - 2, values.length());
            
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
                ResultSet generatedKeys = st.getGeneratedKeys();
                if (generatedKeys.next()) {
                    yeeCourseFiles.setId(generatedKeys.getLong(1));
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
    public Result delete(long id, int schoolId, long courseId) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "DELETE FROM yee_course_files WHERE id = ? AND courseId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, id);
            st.setLong(2, courseId);
            
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

    @Override
    public Result like(int schoolId, long courseId, String name) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "SELECT * FROM yee_course_files WHERE courseId = ? AND name LIKE ? ORDER BY addTime DESC";
            String countSql = "SELECT COUNT(*) FROM yee_course_files WHERE courseId = ? AND name LIKE ?";
            
            String searchName = "%" + name + "%";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            countSt.setString(2, searchName);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setString(2, searchName);
            ResultSet rs = st.executeQuery();
            
            List<YeeCourseFiles> courseFilesList = new ArrayList<>();
            while (rs.next()) {
                YeeCourseFiles courseFiles = rsToYeeCourseFiles(rs);
                courseFilesList.add(courseFiles);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(courseFilesList, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}
