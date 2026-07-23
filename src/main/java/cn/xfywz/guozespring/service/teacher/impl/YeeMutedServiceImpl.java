package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeMuted;
import cn.xfywz.guozespring.entity.dto.YeeMutedQueryParam;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeMutedService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class YeeMutedServiceImpl implements YeeMutedService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeMuted mapMuted(ResultSet rs) throws Exception {
        YeeMuted m = new YeeMuted();
        m.setId(rs.getInt("id"));
        m.setUserId(rs.getInt("userId"));
        m.setUnlockTime(rs.getLong("unlockTime"));
        m.setForum(rs.getString("forum"));
        m.setTeacherId(rs.getInt("teacherId"));
        try {
            m.setAddTime(rs.getTimestamp("addTime"));
        } catch (Exception ignored) {
        }
        m.setContent(rs.getString("content"));
        m.setSchoolId(rs.getInt("schoolId"));
        m.setReplyId(rs.getInt("replyId"));
        return m;
    }

    @Override
    public Result list(int pageSize, int pageNum, int schoolId) {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        // 计算分页偏移量
        int offset = (pageNum - 1) * pageSize;
        String countSql = "SELECT COUNT(*) FROM yee_muted WHERE schoolId = ?";
        String sql = "SELECT * FROM yee_muted WHERE schoolId = ? ORDER BY addTime DESC LIMIT ? OFFSET ?";
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement countSt = connection.prepareStatement(countSql);
             PreparedStatement st = connection.prepareStatement(sql)) {
            // 统计总数
            countSt.setInt(1, schoolId);
            ResultSet rs1 = countSt.executeQuery();
            long total = 0;
            if (rs1.next()) {
                total = rs1.getLong(1);
            }
            rs1.close();
            // 查询数据
            st.setInt(1, schoolId);
            st.setInt(2, pageSize);
            st.setInt(3, offset);
            ResultSet rs = st.executeQuery();
            List<YeeMuted> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapMuted(rs));
            }
            rs.close();
            return Result.success(list, total);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }



    @Override
    public void delete(Integer id, int schoolId) {
        if (id == null || id <= 0) {
            throw new RuntimeException("参数错误");
        }
        // 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new RuntimeException("学校不存在或未审核");
        }

        // 在指定学校的数据库中删除记录
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = connection.prepareStatement("DELETE FROM yee_muted WHERE id = ?")) {
            ps.setInt(1, id);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new RuntimeException("删除失败或记录不存在");
            }
        } catch (Exception e) {
            throw new RuntimeException("删除失败: " + e.getMessage());
        }
    }

    @Override
    public Result searchByCondition(YeeMutedQueryParam param) {
        if (param == null || param.getSchoolId() == null || param.getSchoolId() <= 0) {
            return Result.error("schoolId必填");
        }
        // 查询学校信息并校验是否已审核
        SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        // 设置分页参数，默认第一页，每页10条
        int pageNum = param.getPageNum() == null || param.getPageNum() < 1 ? 1 : param.getPageNum();
        int pageSize = param.getPageSize() == null || param.getPageSize() < 1 ? 10 : param.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        // 构建基础SQL查询语句及参数列表
        StringBuilder base = new StringBuilder(
                " FROM yee_muted ym LEFT JOIN yee_student ys ON ys.id = ym.userId WHERE ym.schoolId = ?");
        List<Object> whereParams = new ArrayList<>();
        whereParams.add(param.getSchoolId());
        if (param.getName() != null && !param.getName().trim().isEmpty()) {
            base.append(" AND ys.name LIKE ?");
            whereParams.add("%" + param.getName().trim() + "%");
        }
        if (param.getIdCard() != null && !param.getIdCard().trim().isEmpty()) {
            base.append(" AND ys.idCard LIKE ?");
            whereParams.add("%" + param.getIdCard().trim() + "%");
        }
        String countSql = "SELECT COUNT(*)" + base;
        String dataSql = "SELECT ym.*" + base + " ORDER BY ym.addTime DESC LIMIT ? OFFSET ?";

        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement countPs = connection.prepareStatement(countSql);
             PreparedStatement dataPs = connection.prepareStatement(dataSql)) {
            // 设置统计参数
            for (int i = 0; i < whereParams.size(); i++) {
                countPs.setObject(i + 1, whereParams.get(i));
            }
            ResultSet countRs = countPs.executeQuery();
            long total = 0;
            if (countRs.next()) total = countRs.getLong(1);
            countRs.close();

            // 设置查询参数
            for (int i = 0; i < whereParams.size(); i++) {
                dataPs.setObject(i + 1, whereParams.get(i));
            }
            dataPs.setInt(whereParams.size() + 1, pageSize);
            dataPs.setInt(whereParams.size() + 2, offset);
            ResultSet rs = dataPs.executeQuery();
            List<YeeMuted> list = new ArrayList<>();
            while (rs.next()) list.add(mapMuted(rs));
            rs.close();
            return Result.success(list, total);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 添加禁言
     */
    @Override
    public void add(YeeMuted muted) {
        // 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(muted.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new RuntimeException("学校不存在或未审核");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            // 先检查该用户是否已经被禁言
            String checkSql = "SELECT COUNT(*) FROM yee_muted WHERE userId = ? AND schoolId = ? AND forum = ? AND unlockTime > ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setLong(1, muted.getUserId());
                checkPs.setLong(2, muted.getSchoolId());
                checkPs.setString(3, muted.getForum() != null ? muted.getForum() : "happy_circle");
                checkPs.setLong(4, System.currentTimeMillis()); // 只检查未过期的禁言

                ResultSet rs = checkPs.executeQuery();
                if (rs.next() && rs.getLong(1) > 0) {
                    throw new RuntimeException("该用户已被禁言，不能重复禁言");
                }
                rs.close();
            }

            // 如果没有被禁言，则执行插入操作
            String sql = "INSERT INTO yee_muted (userId, unlockTime, forum, teacherId, addTime, content, schoolId, replyId) VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, muted.getUserId());
                ps.setLong(2, muted.getUnlockTime());
                ps.setString(3, muted.getForum() != null ? muted.getForum() : "happy_circle");
                ps.setLong(4, muted.getTeacherId());
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                ps.setString(6, muted.getContent());
                ps.setInt(7, Math.toIntExact(muted.getSchoolId()));
                ps.setLong(8, 0);
                int n = ps.executeUpdate();
                if (n <= 0) {
                    throw new RuntimeException("禁言失败");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库操作异常: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("禁言操作异常: " + e.getMessage(), e);
        }
    }

}
