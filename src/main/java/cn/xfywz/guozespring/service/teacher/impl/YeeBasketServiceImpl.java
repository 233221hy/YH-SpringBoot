package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeBasket;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeBasketService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ChengLin
 */
@Service
public class YeeBasketServiceImpl implements YeeBasketService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result selectAll(int schoolId, Integer userId) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            throw new Exception("无法获取数据库连接");
        }

        try {
            // 3. 基础 SQL（只按 userId 过滤）
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_basket WHERE userId = ?");

            // 参数列表
            List<Object> parameters = new ArrayList<>();
            parameters.add(userId);

            // 排序
            sqlBuilder.append(" ORDER BY id DESC");

            // 预编译
            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());

            // 设置参数
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }

            // 执行查询
            ResultSet rs = st.executeQuery();
            List<YeeBasket> list = new ArrayList<>();
            while (rs.next()) {
                YeeBasket basket = new YeeBasket();
                basket.setId(rs.getInt("id"));
                basket.setUserId(rs.getInt("userId"));
                basket.setType(rs.getInt("type"));
                basket.setExId(rs.getInt("exId"));
                basket.setScore(rs.getInt("score"));
                basket.setSchoolId(rs.getInt("schoolId"));
                basket.setRemote(rs.getByte("remote"));
                list.add(basket);
            }
            rs.close();
            st.close();
            Long count = (long) list.size();


            // 返回结果
            return Result.success(list, count);

        } catch (Exception e) {
            throw new Exception("查询试题篮列表失败: " + e.getMessage(), e);
        } finally {
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public Result add(YeeBasket yeeBasket) throws  Exception{
        // 1. 参数校验
        if (yeeBasket == null) {
            return Result.error("参数不能为空");
        }
        if (yeeBasket.getUserId() <= 0) {
            return Result.error("用户ID无效");
        }
        if (yeeBasket.getExId() <= 0) {
            return Result.error("试题ID无效");
        }
        if (yeeBasket.getSchoolId() <= 0) {
            return Result.error("学校ID无效");
        }

        // 2. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeBasket.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            // ✅ 开启事务
            connection.setAutoCommit(false);

            // 4. 检查是否已存在（防重复）—— 未来建议用唯一索引替代
            String checkSql = "SELECT COUNT(*) FROM yee_basket WHERE userId = ? AND exId = ?";
            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                checkSt.setObject(1, yeeBasket.getUserId());
                checkSt.setObject(2, yeeBasket.getExId());

                try (ResultSet checkRs = checkSt.executeQuery()) {
                    if (checkRs.next() && checkRs.getLong(1) > 0) {
                        return Result.error("该试题已在篮筐中");
                    }
                }
            }

            // 5. 插入数据
            String insertSql = "INSERT INTO yee_basket (userId, type, exId, score, schoolId, remote) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertSt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

                insertSt.setObject(1, yeeBasket.getUserId());
                insertSt.setObject(2, yeeBasket.getType());
                insertSt.setObject(3, yeeBasket.getExId());
                insertSt.setObject(4, yeeBasket.getScore() != null ? yeeBasket.getScore() : 0);
                insertSt.setObject(5, yeeBasket.getSchoolId());
                insertSt.setObject(6, yeeBasket.getRemote() != null ? yeeBasket.getRemote() : (byte) 0);

                int rows = insertSt.executeUpdate();

                if (rows == 0) {
                    connection.rollback();
                    return Result.error("添加失败");
                }

                // 获取自增主键
                Integer id = null;
                try (ResultSet generatedKeys = insertSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        id = generatedKeys.getInt(1);
                    }
                }

                // 提交事务
                connection.commit();

                // 构造返回对象
                YeeBasket basket = new YeeBasket();
                basket.setId(id);
                basket.setUserId(yeeBasket.getUserId());
                basket.setType(yeeBasket.getType());
                basket.setExId(yeeBasket.getExId());
                basket.setScore(yeeBasket.getScore());
                basket.setSchoolId(yeeBasket.getSchoolId());
                basket.setRemote(yeeBasket.getRemote());

                return Result.success(basket);

            }

        } catch (SQLException e) {
            // 回滚
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }

            // 判断是否为唯一键冲突（如果加了唯一索引）
            if (e.getErrorCode() == 1062 || e.getMessage().contains("Duplicate entry")) {
                return Result.error("该试题已在篮筐中");
            }
            return Result.error("数据库操作失败，请稍后重试");

        } catch (Exception e) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            return Result.error("系统异常，请联系管理员");

        } finally {
            // 恢复并关闭连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }


    @Override
    public Result delete(int schoolId, int id, Integer userId) throws  Exception{
        // 1. 参数校验
        if (schoolId <= 0) {
            return Result.error("学校ID无效");
        }
        if (id <= 0) {
            return Result.error("记录ID无效");
        }
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        // 2. 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        // 4. 执行删除
        String sql = "DELETE FROM yee_basket WHERE id = ? AND userId = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {

            st.setObject(1, id);
            st.setObject(2, userId);

            int rows = st.executeUpdate();

            if (rows > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("记录不存在或已被删除");
            }

        } catch (SQLException e) {
            return Result.error("删除失败，请稍后重试");
        } catch (Exception e) {
            return Result.error("系统异常，请联系管理员");
        } finally {
            // 安全关闭连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    @Override
    public Result deleteAll(int schoolId, int userId) throws  Exception{
        // 1. 参数校验
        if (schoolId <= 0) {
            return Result.error("学校ID无效");
        }
        if (userId <= 0) {
            return Result.error("用户ID无效");
        }

        // 2. 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        // 4. 执行删除
        String sql = "DELETE FROM yee_basket WHERE schoolId = ? AND userId = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {

            st.setObject(1, schoolId);
            st.setObject(2, userId);

            int rows = st.executeUpdate();

            if (rows > 0) {
                return Result.success("成功删除 " + rows + " 条记录");
            } else {
                return Result.error("无数据可删除");
            }

        } catch (SQLException e) {
            return Result.error("删除失败，请稍后重试");
        } catch (Exception e) {
            return Result.error("系统异常，请联系管理员");
        } finally {
            // 安全关闭连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
            }
        }
    }
}
