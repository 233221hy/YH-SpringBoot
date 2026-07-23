package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.YeeCollegeQueryDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeCollege;
import cn.xfywz.guozespring.entity.vo.YeeCollegeVO;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.service.teacher.YeeCollegeService;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeeCollegeServiceImpl implements YeeCollegeService {

    private final DatabaseUtil databaseUtil;

    // ================ 辅助方法 ================

    /**
     * 构建学院基础查询SQL
     * @return SQL
     */
    private String buildCollegeQuerySql() {
        return "SELECT c.id, c.name, c.allow FROM yee_college c WHERE 1=1";
    }

    /**
     * 学院查询条件
     * @param queryDTO 查询条件
     */
    private void applyCollegeQueryConditions(QueryBuilder queryBuilder, YeeCollegeQueryDTO queryDTO) {
        log.debug("开始应用学院查询条件: {}", queryDTO);

        // 学院名称模糊查询
        if (StringUtils.hasText(queryDTO.getName())) {
            queryBuilder.like("c.name", queryDTO.getName());
        }

        // 审核状态查询
        if (queryDTO.getAllow() != null) {
            queryBuilder.eq("c.allow", queryDTO.getAllow());
        }

        log.debug("学院查询条件应用完成");

    }

    // ================ 执行方法 ================

    /**
     * 查询所有学院列表
     * @param queryDTO 查询条件
     * @return Result
     */
    @Override
    public Result selectAll(YeeCollegeQueryDTO queryDTO) {
        try {
            // 分页查询学院列表
            PageResult<YeeCollegeVO> pageResult = databaseUtil.query(queryDTO.getSchoolId())
                    .sql(buildCollegeQuerySql()) // 构建基础SQL
                    .apply(qb -> applyCollegeQueryConditions(qb, queryDTO)) // 应用查询条件
                    .orderBy("c.id DESC") // 排序
                    .page(YeeCollegeVO::fromResultSet, queryDTO.getPageNum(), queryDTO.getPageSize());

            return Result.success(pageResult.getRows(), pageResult.getTotal());

        } catch (Exception e) {
            log.error("查询学院列表失败: ", e);
            throw new DatabaseException("查询学院列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据id查学院
     */
    @Override
    public Result selectById(int schoolId, long id) throws Exception {
        try {
            Optional<YeeCollegeVO> college = databaseUtil.querySingle(schoolId,
                    "SELECT id, name, allow FROM yee_college WHERE id = ?",
                    YeeCollegeVO::fromResultSet,
                    id);

            return college.map(Result::success).orElse(Result.error("没有此学院"));
        } catch (Exception e) {
            log.error("查询学院失败: ", e);
            throw new DatabaseException("查询学院失败: " + e.getMessage(), e);
        }
    }


    /**
     * 添加学院
     * @param college 学院实体
     * @throws BusinessException
     */
    @Override
    public void add(YeeCollege college) throws BusinessException {
        try {
            Integer schoolId = Math.toIntExact(college.getSchoolId());

            // 检查学院名称是否已存在
            if (databaseUtil.exists(schoolId, "yee_college",
                    "schoolId = ? AND name = ?", schoolId, college.getName())) {
                throw new BusinessException("该学院名称在当前学校已存在");
            }

            // 插入学院数据
            Long generatedId = databaseUtil.update(schoolId)
                    .table("yee_college")
                    .set("schoolId", schoolId)
                    .set("name", college.getName())
                    .set("allow", college.getAllow() != 0 ? 1 : 0)
                    .insert();

            if (generatedId != null) {
                college.setId(generatedId);
            }

        } catch (BusinessException e) {
            throw e; // 业务异常直接抛出
        } catch (Exception e) {
            log.error("添加学院失败", e);
            throw new BusinessException("添加学院失败");
        }
    }

//    @Override
//    public Result add(YeeCollege yeeCollege) throws Exception {
//        SlSchool slSchool = slSchoolMapper.selectById((int)yeeCollege.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() == 0){
//            return Result.error("学校不存在或未审核");
//        }
//
//        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//
//        StringBuilder columns = new StringBuilder("INSERT INTO yee_college (");
//        StringBuilder values = new StringBuilder("VALUES (");
//        ArrayList<Object> parameters = new ArrayList<>();
//
//        // 必填字段：schoolId
//        columns.append("`schoolId`, ");
//        values.append("?, ");
//        parameters.add(yeeCollege.getSchoolId());
//
//        // 动态添加可选字段
//        if (yeeCollege.getName() != null && !yeeCollege.getName().trim().isEmpty()) {
//            columns.append("`name`, ");
//            values.append("?, ");
//            parameters.add(yeeCollege.getName());
//        }
//
//        if (yeeCollege.getAllow() >= 0) {
//            columns.append("`allow`, ");
//            values.append("?, ");
//            parameters.add(yeeCollege.getAllow());
//        }
//
//        // 删除最后的逗号和空格
//        columns.delete(columns.length() - 2, columns.length());
//        values.delete(values.length() - 2, values.length());
//
//        // 构建完整SQL
//        columns.append(") ");
//        values.append(")");
//        String sql = columns.toString() + values.toString();
//
//        PreparedStatement st = connection.prepareStatement(sql);
//        for (int i = 0; i < parameters.size(); i++) {
//            Object param = parameters.get(i);
//            if (param instanceof String) {
//                st.setString(i + 1, (String) param);
//            } else if (param instanceof Long) {
//                st.setLong(i + 1, (Long) param);
//            } else if (param instanceof Integer) {
//                st.setInt(i + 1, (Integer) param);
//            }
//        }
//
//        int rowsInserted = st.executeUpdate();
//        st.close();
//        connection.close();
//
//        if (rowsInserted > 0) {
//            return Result.success("添加成功");
//        } else {
//            return Result.error("添加失败");
//        }
//    }

    /**
     * 更新学院信息
     */
    @Override
    public Result update(YeeCollege yeeCollege) {
        try {
            // 构建并执行更新
            int rowsUpdated = databaseUtil.update((int) yeeCollege.getSchoolId())
                    .table("yee_college")
                    .setIfNotEmpty("name", yeeCollege.getName())
                    .setIfNotNull("allow", yeeCollege.getAllow())
                    .where("id = ?", yeeCollege.getId())
                    .update();

            // 返回结果
            if (rowsUpdated > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败：未找到匹配的记录");
            }

        } catch (Exception e) {
            log.error("更新学院失败: id={}, schoolId={}",
                    yeeCollege.getId(), yeeCollege.getSchoolId(), e);
            throw new DatabaseException("更新学院失败: " + e.getMessage(), e);
        }
    }

//    @Override
//    public Result update(YeeCollege yeeCollege) throws Exception {
//        SlSchool slSchool = slSchoolMapper.selectById((int)yeeCollege.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() == 0){
//            return Result.error("学校不存在或未审核");
//        }
//
//        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//
//        StringBuilder sql = new StringBuilder("UPDATE yee_college SET ");
//        ArrayList<Object> parameters = new ArrayList<>();
//
//        // 动态添加更新字段
//        if (yeeCollege.getName() != null && !yeeCollege.getName().trim().isEmpty()) {
//            sql.append("`name` = ?, ");
//            parameters.add(yeeCollege.getName());
//        }
//
//        if (yeeCollege.getAllow() >= 0) {
//            sql.append("`allow` = ?, ");
//            parameters.add(yeeCollege.getAllow());
//        }
//
//        // 检查是否有可更新的字段
//        if (parameters.isEmpty()) {
//            connection.close();
//            return Result.error("没有可更新的字段");
//        }
//
//        // 删除最后的逗号和空格
//        sql.delete(sql.length() - 2, sql.length());
//
//        // 添加WHERE条件
//        sql.append(" WHERE id = ?");
//        parameters.add(yeeCollege.getId());
//
//        PreparedStatement st = connection.prepareStatement(sql.toString());
//        for (int i = 0; i < parameters.size(); i++) {
//            Object param = parameters.get(i);
//            if (param instanceof String) {
//                st.setString(i + 1, (String) param);
//            } else if (param instanceof Long) {
//                st.setLong(i + 1, (Long) param);
//            } else if (param instanceof Integer) {
//                st.setInt(i + 1, (Integer) param);
//            }
//        }
//
//        int rowsUpdated = st.executeUpdate();
//        st.close();
//        connection.close();
//
//        if (rowsUpdated > 0) {
//            return Result.success("更新成功");
//        } else {
//            return Result.error("更新失败：未找到匹配的记录");
//        }
//    }

    /**
     * 删除学院信息
     */
    @Override
    public Result delete(int schoolId, int id) {
        try {
            int rows = databaseUtil.update(schoolId)
                    .table("yee_college")
                    .eq("id", id)
                    .eq("schoolId", schoolId)
                    .delete();

            if (rows > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败，学院不存在");
            }
        } catch (Exception e) {
            log.error("删除学院失败: id={}, schoolId={}", id, schoolId, e);
            throw new DatabaseException("删除学院失败: " + e.getMessage(), e);
        }
    }


//    @Override
//    public Result delete(int schoolId, int id) throws Exception {
//        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//        if (slSchool == null || slSchool.getAllow() == 0){
//            return Result.error("学校不存在或未审核");
//        }
//
//        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//        String sql = "DELETE FROM yee_college WHERE id = ?";
//        PreparedStatement st = connection.prepareStatement(sql);
//        st.setInt(1, id);
//
//        int rowsDeleted = st.executeUpdate();
//        st.close();
//        connection.close();
//
//        if (rowsDeleted > 0) {
//            return Result.success("删除成功");
//        } else {
//            return Result.error("删除失败：未找到匹配的记录");
//        }
//    }
}
