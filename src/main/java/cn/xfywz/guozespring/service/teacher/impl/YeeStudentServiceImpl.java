package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.dto.YeeStudentQueryDTO;
import cn.xfywz.guozespring.entity.vo.YeeStudentDetailVO;
import cn.xfywz.guozespring.entity.vo.YeeStudentExportVO;
import cn.xfywz.guozespring.entity.vo.YeeStudentListVO;
import cn.xfywz.guozespring.excel.ExcelExportUtil;
import cn.xfywz.guozespring.excel.ExcelImportBuilder;
import cn.xfywz.guozespring.excel.ImportResult;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.exception.ImportExportException;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeStudentService;
import cn.xfywz.guozespring.util.BusinessValidator;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// 新增导入
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import cn.xfywz.guozespring.excel.validation.StudentImportValidationFactory;
import cn.xfywz.guozespring.entity.dto.StudentExcelData;

import java.sql.*;
import java.util.*;

import static cn.xfywz.guozespring.util.EncodePasswordUtil.encodePassword;

@Slf4j
@Service("teacherYeeStudentService")
@RequiredArgsConstructor
public class YeeStudentServiceImpl implements YeeStudentService {

    private final SlSchoolMapper slSchoolMapper;
    private final BusinessValidator businessValidator;
    private final DatabaseUtil databaseUtil;

    // ================ 辅助方法 ================
    /**
     * 构建学生基础查询SQL
     * @param isExport 是否是导出
     *                 true: 导出
     * @return SQL
     */
    private String buildQuerySql(boolean isExport) {
        String selectCols = isExport ?
                "s.number, s.name, s.gender, s.idCard, s.email, s.mobile, co.name AS collegeName, cl.name AS className" :
                "s.id, s.number, s.name, s.idCard, s.gender, s.mobile, s.entryYear, co.name AS collegeName, cl.name AS className, s.addDate";

        return "SELECT " + selectCols +
                """
                 FROM yee_student s
                LEFT JOIN yee_college co ON co.id = s.collegeId
                LEFT JOIN yee_classes cl ON cl.id = s.classId
                """;
    }

    /**
     * 学生查询条件
     *
     * @param queryDTO 查询条件
     */
    private void applyQueryConditions(QueryBuilder queryBuilder, YeeStudentQueryDTO queryDTO) {
        log.debug("开始应用查询条件: {}", queryDTO);

        // 关键字查询（学号或姓名）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = "%" + queryDTO.getKeyword() + "%";
            queryBuilder.where("(s.number LIKE ? OR s.name LIKE ?)", keyword, keyword);
        }

        // 身份证精确查询
        if (StringUtils.hasText(queryDTO.getIdCard())) {
            queryBuilder.eq("s.idCard", queryDTO.getIdCard());
        }

        // 入学年份
        if (queryDTO.getEntryYear() != null) {
            queryBuilder.eq("s.entryYear", queryDTO.getEntryYear());
        }

        // 性别
        if (StringUtils.hasText(queryDTO.getGender())) {
            queryBuilder.eq("s.gender", queryDTO.getGender());
        }

        // 学院
        if (queryDTO.getCollegeId() != null) {
            queryBuilder.eq("s.collegeId", queryDTO.getCollegeId());
        }

        // 班级
        if (queryDTO.getClassId() != null) {
            queryBuilder.eq("s.classId", queryDTO.getClassId());
        }

        log.debug("查询条件应用完成");

    }

    // ================ 执行方法 ================

     /**
      * 查询所有学生列表
      * @param queryDTO 查询条件
      * @return Result
      */
    @Override
    public Result selectAll(YeeStudentQueryDTO queryDTO) {
        // 分页查询
        PageResult<YeeStudentListVO> pageResult = databaseUtil.query(queryDTO.getSchoolId())
                .sql(buildQuerySql(false)) // 构建基础SQL
                .apply(qb -> applyQueryConditions(qb, queryDTO)) // 应用查询条件
                .orderBy("s.id DESC") // 排序
                .page(YeeStudentListVO::fromResultSet, queryDTO.getPageNum(), queryDTO.getPageSize());
        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

    /**
     * 导出学生数据
     * @param queryDTO 查询条件
     * @param response HttpServletResponse
     * @throws ImportExportException
     */
    @Override
    public void exportData(YeeStudentQueryDTO queryDTO, HttpServletResponse response) {
        try {
            // 查询所有数据（不分页）
            List<YeeStudentExportVO> exportList = databaseUtil.query(queryDTO.getSchoolId())
                    .sql(buildQuerySql(true)) // 构建导出SQL
                    .apply(qb -> applyQueryConditions(qb, queryDTO)) // 应用查询条件
                    .orderBy("s.id DESC") // 排序
                    .list(YeeStudentExportVO::fromResultSet); // 查询列表

            ExcelExportUtil.exportWithPreprocess(exportList, response, YeeStudentExportVO.class);

        } catch (Exception ex) {
            log.error("导出数据失败", ex);
            throw new ImportExportException("导出数据失败: " + ex.getMessage(), ex);
        }
    }


    /**
     * 导入学生数据
     * @param schoolId 学校ID
     * @param file Excel文件
     * @return Result
     * @throws Exception
     */
    @Override
    public Result importData(int schoolId, MultipartFile file) {
        long startTime = System.currentTimeMillis();

        // 参数校验
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String defaultHashedPassword = encodePassword("a123456");

        try {
            ImportResult result = databaseUtil.executeInTransaction(schoolId, conn -> {
                try {
                    // 事务内加载缓存
                    Map<String, Long> collegeMap = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, name FROM yee_college WHERE schoolId = ?")) {
                    ps.setInt(1, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString("name");
                            if (name != null) collegeMap.put(name, rs.getLong("id"));
                        }
                    }
                }

                Map<String, Long> classMap = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, name FROM yee_classes WHERE schoolId = ?")) {
                    ps.setInt(1, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString("name");
                            if (name != null) classMap.put(name, rs.getLong("id"));
                        }
                    }
                }

                Set<String> existingNumbers = new HashSet<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT number FROM yee_student WHERE schoolId = ?")) {
                    ps.setInt(1, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String num = rs.getString("number");
                            if (num != null && !num.isBlank()) existingNumbers.add(num);
                        }
                    }
                }

                var ctx = StudentImportValidationFactory.createContext(existingNumbers, collegeMap, classMap);

                ImportResult r = ExcelImportBuilder
                        .of(StudentExcelData.class)
                        .from(file.getInputStream())
                        .preprocess(StudentExcelData::cleanData)
                        .businessValidator(StudentImportValidationFactory.createBusinessValidator(ctx))
                        .batchPersist(batch -> {
                            String sql = """
                                    INSERT INTO yee_student
                                    (`schoolId`,`addTime`,`number`,`name`,`gender`,`idCard`,`email`,`mobile`,`collegeId`,`classId`,`entryYear`,`password`)
                                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                                    """;
                            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                Timestamp now = new Timestamp(System.currentTimeMillis());
                                for (StudentExcelData row : batch) {
                                    Long collegeIdVal = collegeMap.get(row.getCollege());
                                    Long classIdVal = classMap.get(row.getStuClass());
                                    long entryYear = Long.parseLong(row.getGrade());

                                    ps.setLong(1, schoolId);
                                    ps.setTimestamp(2, now);
                                    ps.setString(3, row.getNumber());
                                    ps.setString(4, row.getName());
                                    ps.setString(5, row.getGender());
                                    ps.setString(6, row.getIdCard());
                                    ps.setString(7, row.getEmail());
                                    ps.setString(8, row.getPhone());
                                    ps.setLong(9, collegeIdVal);
                                    ps.setLong(10, classIdVal);
                                    ps.setLong(11, entryYear);
                                    ps.setString(12, defaultHashedPassword);
                                    ps.addBatch();
                                }
                                int[] results = ps.executeBatch();
                                int inserted = 0;
                                for (int rc : results) {
                                    if (rc > 0 || rc == Statement.SUCCESS_NO_INFO) inserted++;
                                }
                                return inserted;
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .execute();

                if (!r.isSuccess()) {
                        throw new ImportExportException(r.getFailMessage("部分数据校验失败，已全部回滚"));
                    }
                    return r;
                } catch (ImportExportException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (result.isSuccess()) {
                return Result.success("导入成功", (Object) result.toMap());
            } else {
                return Result.error(result.getFailMessage("导入失败"), result.toMap());
            }
        } catch (Exception e) {
            // 从异常链中提取 ImportExportException 的真实原因
            Throwable cause = e;
            while (cause.getCause() != null && !(cause instanceof ImportExportException)) {
                cause = cause.getCause();
            }
            if (cause instanceof ImportExportException) {
                log.error("导入失败: {}", cause.getMessage());
                return Result.error(cause.getMessage());
            }
            log.error("导入失败: " + e.getMessage(), e);
            return Result.error("导入失败：" + e.getMessage(),
                    ImportResult.systemError(e.getMessage(), System.currentTimeMillis() - startTime).toMap());
        }
    }

    /**
     * 检查班级是否有学生
     * @param schoolId 学校id
     * @param classId 班级id
     * @return true:有学生，false:没有学生
     * @throws Exception
     */
    @Override
    public boolean hasStudentByClassId(int schoolId, Long classId) {
        try {
            return databaseUtil.query(schoolId)
                .sql("SELECT * FROM yee_student WHERE 1=1")
                .eq("classId", classId)
                .eq("schoolId", schoolId)
                .exists(); // 自动构建: SELECT COUNT(*) FROM yee_student WHERE 1=1 AND classId = ? AND schoolId = ?

        } catch (Exception e) {
            log.error("检查班级是否有学生失败: schoolId={}, classId={}", schoolId, classId, e);
            return false;
        }
    }

    /**
     * 查询学生详情
     * @param schoolId 学校id
     * @param id 学生id
     * @return Result
     * @throws DatabaseException
     */
    @Override
    public Result selectById(int schoolId, long id) {
        try {
            // 查询学生详情
            Optional<YeeStudentDetailVO> student = databaseUtil.query(schoolId)
                .sql("SELECT * FROM yee_student WHERE id = ?")
                .param(id)
                .single(YeeStudentDetailVO::fromResultSet); // 使用single()方法查询单条

            return student.map(Result::success)
                .orElseGet(() -> Result.error("没有此学生"));

        }catch (DatabaseException e) {
            log.error("查询学生详情失败: schoolId={}, id={}", schoolId, id, e);
            return Result.error("查询学生信息失败");
        }
    }


    /**
     * 添加学生
     * @param s 学生信息
     * @throws Exception
     */
    @Override
    public void add(YeeStudent s) throws Exception {
        try {
            int schoolId = (int) s.getSchoolId();

            // 检查学号是否已存在
            businessValidator.validateStuNumberUnique(databaseUtil, schoolId, s.getNumber(), null);
            // 检查身份证是否已存在
            businessValidator.validateStuIdCardUnique(databaseUtil, schoolId, s.getIdCard(), null);
//            if (databaseUtil.exists(schoolId, "yee_student",
//                    "schoolId = ? AND number = ?", schoolId, s.getNumber())) {
//                throw new BusinessException("该学号在当前学校已存在");
//            }

            // 插入
            Long generatedId = databaseUtil.update(schoolId)
                    .table("yee_student")
                    .set("schoolId", schoolId)
                    .set("addTime", s.getAddTime() != null ? s.getAddTime()
                            : new Timestamp(System.currentTimeMillis()))
                    .set("number", s.getNumber())
                    .set("name", s.getName())
                    .set("idCard", s.getIdCard())
                    .set("entryYear", s.getEntryYear())
                    .set("password", encodePassword(
                            (s.getPassword() != null && !s.getPassword().isEmpty())
                                    ? s.getPassword()
                                    : "a123456"
                    ))
                    .setIfNotEmpty("gender", s.getGender())
                    .setIfNotEmpty("mobile", s.getMobile())
                    .setIfNotEmpty("weChat", s.getWeChat())
                    .setIfNotEmpty("email", s.getEmail())
                    .setIfPositive("collegeId", s.getCollegeId())
                    .setIfPositive("classId", s.getClassId())
                    .insert();  // 直接调用insert()方法

            if (generatedId != null) {
                s.setId(generatedId);
            }

        } catch (Exception e) {
            throw new BusinessException("添加学生失败");
        }
    }


    /**
     * 修改学生信息
     * @param s 学生信息
     * @return Result
     * @throws BusinessException
     * @throws DatabaseException
     */
    @Override
    public Result update(YeeStudent s) {
        try {
            int schoolId = (int) s.getSchoolId();

            // 更新
            int rows = databaseUtil.update(schoolId)
                    .table("yee_student")
                    .setIfNotEmpty("number", s.getNumber())
                    .setIfNotEmpty("name", s.getName())
                    .setIfNotEmpty("avatar", s.getAvatar())
                    .setIfNotEmpty("idCard", s.getIdCard())
                    .setIfNotEmpty("gender", s.getGender())
                    .setIfPositive("entryYear", s.getEntryYear())
                    .setIfNotEmpty("mobile", s.getMobile())
                    .setIfNotEmpty("weChat", s.getWeChat())
                    .setIfNotEmpty("email", s.getEmail())
                    .apply(builder -> {
                        // 密码需要特殊处理（加密）
                        if (StringUtils.hasText(s.getPassword())) {
                            builder.set("password", encodePassword(s.getPassword()));
                        }
                    })
                    .setIfPositive("province", s.getProvince())
                    .setIfPositive("city", s.getCity())
                    .setIfPositive("region", s.getRegion())
                    .setIfNotEmpty("address", s.getAddress())
                    .setIfNotEmpty("intro", s.getIntro())
                    .setIfPositive("point", s.getPoint())
                    .setIfNotEmpty("signature", s.getSignature())
                    .setIfPositive("classId", s.getClassId())
                    .setIfPositive("collegeId", s.getCollegeId())
                    .eq("id", s.getId())
                    .eq("schoolId", schoolId)
                    .update();

            if (rows > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败，学生不存在或数据未变化");
            }
        } catch (Exception e) {
            log.error("更新学生未知异常: id={}", s.getId(), e);
            return Result.error("更新失败");
        }
    }

    /**
     * 删除学生
     */
    @Override
    public void delete(Long id, int schoolId) throws Exception {
        try {
            int rows = databaseUtil.update(schoolId)
                .table("yee_student")
                .eq("id", id)
                .eq("schoolId", schoolId)
                .delete();

            if (rows == 0) {
                throw new BusinessException("删除失败，学生不存在");
            }

        } catch (Exception e) {
            log.error("删除学生失败: id={}, schoolId={}", id, schoolId, e);
            throw new BusinessException("删除失败");
        }
    }


    /**
     * 密码重置
     */
    public void passwordReset(int schoolId, List<String> stuNumbers) {
        String defaultPassword = encodePassword("a123456");

        for (String number : stuNumbers) {
            databaseUtil.update(schoolId)
                    .table("yee_student")
                    .set("password", defaultPassword)
                    .eq("number", number)
                    .eq("schoolId", schoolId)
                    .update();
        }

        log.debug("密码重置完成: schoolId={}, count={}", schoolId, stuNumbers.size());
    }

//    @Override
//    public void passwordReset(int schoolId, List<String> number) {
//        // 查询允许操作的学校信息
//        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//        if (slSchool == null || slSchool.getAllow() == 0) {
//            throw new RuntimeException("学校不存在或未审核");
//        }
//
//        // 检查ID列表是否为空
//        if (number == null || number.isEmpty()) {
//            throw new RuntimeException("学生ID列表不能为空");
//        }
//
//        // 构建SQL更新语句
//        StringBuilder sqlBuilder = new StringBuilder("UPDATE yee_student SET password = ? WHERE number IN (");
//        for (int i = 0; i < number.size(); i++) {
//            sqlBuilder.append("?");
//            if (i < number.size() - 1) {
//                sqlBuilder.append(",");
//            }
//        }
//        sqlBuilder.append(")");
//
//        // 密码加密
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        String encodedPassword = encoder.encode("a123456");
//
//        // 执行SQL更新语句
//        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
//             PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())) {
//
//            // 设置密码参数
//            ps.setString(1, encodedPassword);
//
//            // 设置ID参数
//            for (int i = 0; i < number.size(); i++) {
//                ps.setString(i + 2, number.get(i)); // 注意索引从2开始，因为第一个参数是密码
//            }
//
//            // 执行更新
//            int rowsUpdated = ps.executeUpdate();
//
//            // 检查更新结果
//            if (rowsUpdated == 0) {
//                throw new RuntimeException("更新失败：未找到匹配的学生记录");
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("密码重置失败: " + e.getMessage(), e);
//        }
//    }


}
