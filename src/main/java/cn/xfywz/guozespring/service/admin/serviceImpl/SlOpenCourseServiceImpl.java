package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.dto.SlOpenCourseQueryDTO;
import cn.xfywz.guozespring.entity.mhmain.*;
import cn.xfywz.guozespring.entity.vo.OpenChapterNode;
import cn.xfywz.guozespring.entity.vo.OpenCourseChapterNode;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.mapper.*;
import cn.xfywz.guozespring.service.admin.SlOpenCourseService;
import cn.xfywz.guozespring.service.admin.SlSchoolThematicService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;


@Service
public class SlOpenCourseServiceImpl implements SlOpenCourseService {
    @Autowired
    private SlOpenCourseMapper slOpenCourseMapper;
    @Autowired
    private SlOpenChapterMapper slOpenChapterMapper;
    @Autowired
    private SlOpenNodeMapper slOpenNodeMapper;
    @Autowired
    private SlOpenNodeFilesMapper slOpenNodeFilesMapper;

    @Override
    public SlOpenCourse selectNameById(int id) {
        return slOpenCourseMapper.selectNameById(id);
    }

    @Override
    public Result selectById(int id) {
        OpenCourseChapterNode openCourseChapterNode = new OpenCourseChapterNode();
        List<SlOpenChapter> slOpenChapters=new ArrayList<>();
        SlOpenCourse slOpenCourse= slOpenCourseMapper.selectById(id);
        if (slOpenCourse != null) {
            openCourseChapterNode.setId(slOpenCourse.getId());
            openCourseChapterNode.setName(slOpenCourse.getName());
            openCourseChapterNode.setCode(slOpenCourse.getCode());
            openCourseChapterNode.setCategoryId(slOpenCourse.getCategoryId());
            openCourseChapterNode.setCateBid(slOpenCourse.getCateBid());
            openCourseChapterNode.setCateMid(slOpenCourse.getCateMid());
            openCourseChapterNode.setCover(slOpenCourse.getCover());
            openCourseChapterNode.setIntro(slOpenCourse.getIntro());
            openCourseChapterNode.setContent(slOpenCourse.getContent());
            openCourseChapterNode.setAllow(slOpenCourse.getAllow());
            openCourseChapterNode.setAddTime(slOpenCourse.getAddTime());
            openCourseChapterNode.setMode(slOpenCourse.getMode());
            openCourseChapterNode.setWeek(slOpenCourse.getWeek());
            openCourseChapterNode.setTimes(slOpenCourse.getTimes());
            openCourseChapterNode.setStartTime(slOpenCourse.getStartTime());
            openCourseChapterNode.setCategoryItem(slOpenCourse.getCategoryItem());
            openCourseChapterNode.setClusterId(slOpenCourse.getClusterId());
            openCourseChapterNode.setEndTime(slOpenCourse.getEndTime());
            openCourseChapterNode.setSignStartTime(slOpenCourse.getSignStartTime());
            openCourseChapterNode.setSignEndTime(slOpenCourse.getSignEndTime());
            openCourseChapterNode.setMark(slOpenCourse.getMark());
            openCourseChapterNode.setWeight(slOpenCourse.getWeight());
            openCourseChapterNode.setLecturer(slOpenCourse.getLecturer());
            openCourseChapterNode.setOrganization(slOpenCourse.getOrganization());
            openCourseChapterNode.setTeacherIntro(slOpenCourse.getTeacherIntro());
            openCourseChapterNode.setViewRate(slOpenCourse.getViewRate());
            openCourseChapterNode.setStuCount(slOpenCourse.getStuCount());
            openCourseChapterNode.setPeriodName(slOpenCourse.getPeriodName());
            openCourseChapterNode.setCreateId(slOpenCourse.getCreateId());
            openCourseChapterNode.setSchoolId(slOpenCourse.getSchoolId());
            openCourseChapterNode.setOpen(slOpenCourse.getOpen());
            openCourseChapterNode.setSchoolAllow(slOpenCourse.getSchoolAllow());
            openCourseChapterNode.setFree(slOpenCourse.getFree());
            openCourseChapterNode.setPrice(slOpenCourse.getPrice());
            openCourseChapterNode.setDbLock(slOpenCourse.getDbLock());
            openCourseChapterNode.setClick(slOpenCourse.getClick());
        }
        QueryWrapper<SlOpenChapter> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("courseId",id);
        List<SlOpenChapter> slOpenChapter= slOpenChapterMapper.selectList(queryWrapper);
        for (SlOpenChapter slOpenChapter1 : slOpenChapter){
            OpenChapterNode openChapterNode1 = new OpenChapterNode();
            openChapterNode1.setId(slOpenChapter1.getId());
            openChapterNode1.setName(slOpenChapter1.getName());
            openChapterNode1.setCourseId(slOpenChapter1.getCourseId());
            openChapterNode1.setSort(slOpenChapter1.getSort());
            openChapterNode1.setSchoolId(slOpenChapter1.getSchoolId());
            QueryWrapper<SlOpenNode> queryWrapper1 = new QueryWrapper<>();
            queryWrapper1.eq("chapterId", slOpenChapter1.getId());
            openChapterNode1.setSlOpenNodes(slOpenNodeMapper.selectList(queryWrapper1));
            slOpenChapters.add(openChapterNode1);
        }
        openCourseChapterNode.setSlOpenChapters(slOpenChapters);
        return Result.success(openCourseChapterNode);
    }

    @Override
    public Result add(SlOpenCourse slOpenCourse) {
        slOpenCourse.setAddTime(new Timestamp(System.currentTimeMillis()));
        if (slOpenCourseMapper.insert(slOpenCourse)==0){
            return Result.error("添加失败");
        }else return Result.success("添加成功");
    }

    @Override
    public Result del(int id) {
        if (slOpenCourseMapper.deleteById(id)==0){
            return Result.error("删除失败");
        }else return Result.success("删除成功");
    }

    @Override
    public Result update(SlOpenCourse slOpenCourse) {
        if (slOpenCourseMapper.updateById(slOpenCourse)==0){
            return Result.error("修改失败");
        }else return Result.success("修改成功");
    }


    @Override
    public Result selectLike(TplCourseLike courseLike, int pageSize, int pageNum) {
        Page<SlOpenCourse> page = new Page<>(pageNum, pageSize);

        QueryWrapper<SlOpenCourse> qw = new QueryWrapper<>();
        if (StringUtils.isNotBlank(courseLike.getName())) {
            qw.like("name", courseLike.getName());
        }
        if (StringUtils.isNotBlank(courseLike.getCode())) {
            qw.like("code", courseLike.getCode());
        }
        if (courseLike.getCateBid() != null) {
            qw.like("cateBid", courseLike.getCateBid());
        }
        if (courseLike.getCateMid() != null) {
            qw.like("cateMid", courseLike.getCateMid());
        }
        if (courseLike.getSchoolId() != null) {
            qw.like("schoolId", courseLike.getSchoolId());
        }

        Page<SlOpenCourse> result = slOpenCourseMapper.selectPage(page, qw);
        return Result.success(result.getRecords(), result.getTotal());
    }

    @Override
    public Result publish(int id) {
        // 1. 查询课程是否存在
        SlOpenCourse course = slOpenCourseMapper.selectById(id);
        if (course == null) {
            return Result.error("课程不存在");
        }

        // 2. 切换发布状态：0 ↔ 1
        Long currentAllow = course.getAllow();
        int newAllow = (currentAllow == null || currentAllow == 0) ? 1 : 0;

        // 3. 构造更新对象（只更新 allow 字段，避免覆盖其他字段）
        SlOpenCourse updateObj = new SlOpenCourse();
        updateObj.setId(id);
        updateObj.setAllow(newAllow);

        // 4. 执行更新
        int rows = slOpenCourseMapper.updateById(updateObj);
        if (rows == 0) {
            return Result.error("发布状态更新失败");
        }

        // 5. 返回结果（可附带当前状态）
        String message = (newAllow == 1) ? "课程已发布" : "课程已下架";
        return Result.success(message);
    }

    /**
     * 查询对应学校的公开课
     */
    @Override
    public Result openCourseList(SlOpenCourseQueryDTO queryDTO, int schoolId) {
        //  构建公开课查询条件
        QueryWrapper<SlOpenCourse> courseWrapper = new QueryWrapper<>();

        // 必须条件：学校ID和允许状态
        courseWrapper.eq("schoolId", schoolId);
//                .eq("allow", 1);

        // 条件参数：课程名称（模糊查询）
        if (StringUtils.isNotBlank(queryDTO.getName())) {
            courseWrapper.like("name", queryDTO.getName());
        }

        // 条件参数：学科一级分类
        if (StringUtils.isNotBlank(queryDTO.getCateBid())) {
            courseWrapper.eq("cateBid", queryDTO.getCateBid());
        }
        // 条件参数：学科二级分类
        if (StringUtils.isNotBlank(queryDTO.getCateMid())) {
            courseWrapper.eq("cateMid", queryDTO.getCateMid());
        }
        // 课程编号
        if (StringUtils.isNotBlank(queryDTO.getCode())) {
            courseWrapper.like("code", queryDTO.getCode());
        }

        // 条件参数：审核状态
        if (queryDTO.getState() != null) {
            int state = queryDTO.getState();

            switch (state) {
                case 0: // 未发布
                    courseWrapper.eq("allow", 0)
                            .eq("schoolAllow", 0);
                    break;
                case 1: // 待审核
                    courseWrapper.eq("allow", 1)
                            .eq("schoolAllow", 0);
                    break;
                case 2: // 已发布
                    courseWrapper.eq("allow", 1)
                            .eq("schoolAllow", 1);
                    break;
                default:

                    break;
            }
        }

        // 排序和分页
//        courseWrapper.orderByDesc("addTime");
        courseWrapper.apply("1=1")  // 占位符
                     .orderByDesc("CASE WHEN weight > 0 THEN 1 ELSE 2 END") //权重优先级排序
                     .orderByDesc("CASE WHEN weight > 0 THEN weight ELSE 0 END") //权重值排序
                     .orderByDesc("addTime");

        //  执行分页查询
        IPage<SlOpenCourse> page = new Page<>(
                queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum(),
                queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize()
        );

        page = slOpenCourseMapper.selectPage(page, courseWrapper);

        return Result.success(page.getRecords(), page.getTotal());
    }



    /**
     * 公开课程复制（核心入口方法）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result openCourseTemplateImport(long tplId, SlOpenCourse input) {
        // 1. 校验模板课程是否存在
        SlOpenCourse tplCourse = slOpenCourseMapper.selectById(tplId);
        if (tplCourse == null) {
            return Result.error("课程不存在，无法复制课程结构");
        }

        // 2. 准备公开课程主表数据（前端传值覆盖，不传用模板值）
        SlOpenCourse targetCourse = prepareOpenCourse(input, tplCourse);

        // 3. 插入公开课程主表
        int insertCount = slOpenCourseMapper.insert(targetCourse);
        Long newOpenCourseId = targetCourse.getId();
        if (insertCount <= 0 || newOpenCourseId == null || newOpenCourseId <= 0) {
            throw new RuntimeException("公开课程插入失败，未生成ID");
        }

        // 4. 复制模板的章、节、文件结构到新课程（转换为int类型）
        try {
            copyStructureFromTemplate(tplId, newOpenCourseId.intValue(), (int) targetCourse.getSchoolId());
        } catch (Exception e) {
            throw new RuntimeException("复制课程章节目录失败：" + e.getMessage());
        }

        return Result.success("公开课程创建成功");
    }

    /**
     * 构建公开课程对象（适配int类型字段）
     */
    private SlOpenCourse prepareOpenCourse(SlOpenCourse input, SlOpenCourse tplCourse) {
        if (input == null) {
            throw new IllegalArgumentException("前端传入的课程信息不能为空");
        }
        if (tplCourse == null) {
            throw new IllegalArgumentException("模板课程信息不能为空");
        }

        SlOpenCourse course = new SlOpenCourse();

        // 核心非空校验
        if (input.getSchoolId() <= 0) {
            throw new IllegalArgumentException("schoolId 必须大于0");
        }
        if (input.getCreateId() <= 0) {
            throw new IllegalArgumentException("createId 必须大于0");
        }

        // 基础信息（前端传值覆盖，不传用模板值）
        course.setName(input.getName() != null ? input.getName() : tplCourse.getName());
        course.setCode(input.getCode() != null ? input.getCode() : tplCourse.getCode());
        course.setCategoryId(input.getCategoryId() != null ? input.getCategoryId() : tplCourse.getCategoryId());
        course.setCateBid(input.getCateBid() > 0 ? input.getCateBid() : tplCourse.getCateBid());
        course.setCateMid(input.getCateMid() > 0 ? input.getCateMid() : tplCourse.getCateMid());
        course.setCover(input.getCover() != null ? input.getCover() : tplCourse.getCover());
        course.setIntro(input.getIntro() != null ? input.getIntro() : tplCourse.getIntro());
        course.setContent(input.getContent() != null ? input.getContent() : tplCourse.getContent());
        course.setLecturer(input.getLecturer() != null ? input.getLecturer() : tplCourse.getLecturer());
        course.setOrganization(input.getOrganization() != null ? input.getOrganization() : tplCourse.getOrganization());
        course.setTeacherIntro(input.getTeacherIntro() != null ? input.getTeacherIntro() : tplCourse.getTeacherIntro());
        course.setClusterId(input.getClusterId() > 0 ? input.getClusterId() : tplCourse.getClusterId());
        course.setCategoryItem(input.getCategoryItem() != null ? input.getCategoryItem() : tplCourse.getCategoryItem());
        course.setMode(input.getMode() > 0 ? input.getMode() : tplCourse.getMode());
        course.setWeek(input.getWeek() > 0 ? input.getWeek() : tplCourse.getWeek());
        course.setTimes(input.getTimes() > 0 ? input.getTimes() : tplCourse.getTimes());
        course.setPeriodName(input.getPeriodName() != null ? input.getPeriodName() : tplCourse.getPeriodName());
        course.setViewRate(input.getViewRate() > 0 ? input.getViewRate() : tplCourse.getViewRate());
        course.setStuCount(input.getStuCount() > 0 ? input.getStuCount() : tplCourse.getStuCount());
        course.setClick(input.getClick() > 0 ? input.getClick() : tplCourse.getClick());
        course.setPrice(input.getPrice() > 0 ? input.getPrice() : tplCourse.getPrice());
        course.setMark(input.getMark() > 0 ? input.getMark() : tplCourse.getMark());
        course.setWeight(input.getWeight() > 0 ? input.getWeight() : tplCourse.getWeight());

        // 状态字段
        course.setAllow(tplCourse.getAllow());
        course.setFree(tplCourse.getFree());
        course.setDbLock(tplCourse.getDbLock());
        course.setOpen(tplCourse.getOpen());
        course.setSchoolAllow(tplCourse.getSchoolAllow());

        // 时间字段（核心修复：前端传值覆盖，不传用模板值）
        course.setAddTime(new Timestamp(System.currentTimeMillis()));
        course.setStartTime(input.getStartTime() != null ? input.getStartTime() : tplCourse.getStartTime());
        course.setEndTime(input.getEndTime() != null ? input.getEndTime() : tplCourse.getEndTime());
        course.setSignStartTime(input.getSignStartTime() != null ? input.getSignStartTime() : tplCourse.getSignStartTime());
        course.setSignEndTime(input.getSignEndTime() != null ? input.getSignEndTime() : tplCourse.getSignEndTime());

        // 上下文字段（int类型）
        course.setSchoolId(input.getSchoolId());
        course.setCreateId(input.getCreateId());

        return course;
    }

    /**
     * 从模板复制章、节、文件结构（最终版：修复节点文件nodeId赋值）
     * @param tplId 模板课程ID（long）
     * @param newCourseId 新课程ID（int，适配数据库）
     * @param schoolId 学校ID（int，适配数据库）
     */
    private void copyStructureFromTemplate(Long tplId, int newCourseId, int schoolId) {
        // 1. 参数前置校验
        if (tplId == null || newCourseId <= 0 || schoolId <= 0) {
            throw new IllegalArgumentException("复制结构参数无效：tplId=" + tplId + ", newCourseId=" + newCourseId + ", schoolId=" + schoolId);
        }

        // ========== 第一步：复制章节（适配int类型） ==========
        List<SlOpenChapter> tplChapters = slOpenChapterMapper.selectByCourseId(tplId.intValue());
        if (CollectionUtils.isEmpty(tplChapters)) {
            return;
        }

        Map<Long, Long> chapterIdMap = new HashMap<>(); // 模板章节ID(int) -> 新章节ID(int)
        for (SlOpenChapter tplChapter : tplChapters) {
            SlOpenChapter newChapter = new SlOpenChapter();
            newChapter.setName(tplChapter.getName());
            newChapter.setCourseId(newCourseId); // 直接赋值int类型
            newChapter.setSort(tplChapter.getSort());
            newChapter.setSchoolId(schoolId);    // 直接赋值int类型

            // 插入章节并校验结果
            int chapterInsertCount = slOpenChapterMapper.insert(newChapter);
            Long newChapterId = newChapter.getId();
            if (chapterInsertCount <= 0 || newChapterId == null) {
                throw new RuntimeException("复制章节失败，模板章节ID：" + tplChapter.getId());
            }
            chapterIdMap.put(tplChapter.getId(), newChapterId);
        }

        // ========== 第二步：复制节点（核心修复chapterId赋值） ==========
        Set<Long> oldChapterIds = chapterIdMap.keySet();
        List<SlOpenNode> tplNodes = slOpenNodeMapper.selectByChapterIds(oldChapterIds);
        if (CollectionUtils.isEmpty(tplNodes)) {
            return;
        }

        Map<Long, Long> nodeIdMap = new HashMap<>(); // 模板节点ID(int) -> 新节点ID(int)
        for (SlOpenNode tplNode : tplNodes) {
            // 1. 核心修复：获取新章节ID（int类型）
            Long newChapterId = chapterIdMap.get(tplNode.getChapterId());
            if (newChapterId == null) {
                continue;
            }

            SlOpenNode newNode = new SlOpenNode();
            newNode.setName(tplNode.getName());
            newNode.setType(tplNode.getType());
            newNode.setTabVideo(tplNode.getTabVideo());
            newNode.setTabFile(tplNode.getTabFile());
            newNode.setTabVote(tplNode.getTabVote());
            newNode.setTabWork(tplNode.getTabWork());
            newNode.setTabExam(tplNode.getTabExam());

            // 2. 核心修复：chapterId赋值（int类型，匹配数据库）
            newNode.setChapterId(newChapterId);
            // 3. courseId赋值（int类型）
            newNode.setCourseId(newCourseId);
            newNode.setVideoFile(tplNode.getVideoFile());
            newNode.setVideoDuration(tplNode.getVideoDuration());
            newNode.setLocalFile(tplNode.getLocalFile());
            newNode.setVotingPath(tplNode.getVotingPath());
            newNode.setSort(tplNode.getSort());
            newNode.setVideoMode(tplNode.getVideoMode());
            newNode.setSchoolId(schoolId); // int类型

            // 插入节点并校验结果
            int nodeInsertCount = slOpenNodeMapper.insert(newNode);
            Long newNodeId = newNode.getId();
            if (nodeInsertCount <= 0 || newNodeId == null) {
                throw new RuntimeException("复制节点失败，模板节点ID：" + tplNode.getId());
            }
            nodeIdMap.put(tplNode.getId(), newNodeId);
        }

        // ========== 第三步：复制节点文件（核心修复nodeId赋值） ==========
        Set<Long> oldNodeIds = nodeIdMap.keySet();
        List<SlOpenNodeFiles> tplFiles = slOpenNodeFilesMapper.selectByNodeIds(oldNodeIds);
        if (CollectionUtils.isEmpty(tplFiles)) {
            return;
        }

        for (SlOpenNodeFiles tplFile : tplFiles) {
            // 1. 核心修复：获取新节点ID（int类型，确保nodeId有值）
            Long newNodeId = nodeIdMap.get(tplFile.getNodeId());
            if (newNodeId == null) {
                continue;
            }

            SlOpenNodeFiles newFile = new SlOpenNodeFiles();
            // 2. 核心修复：nodeId赋值（int类型，匹配数据库sl_open_node_files表）
            newFile.setNodeId(newNodeId);
            // 3. courseId赋值（int类型）
            newFile.setCourseId(newCourseId);
            // 4. schoolId赋值（int类型）
            newFile.setSchoolId(schoolId);
            // 其他文件字段赋值
            newFile.setName(tplFile.getName() != null ? tplFile.getName() : tplFile.getFileName());
            newFile.setFileName(tplFile.getFileName());
            newFile.setUploadPath(tplFile.getUploadPath());
            newFile.setTimeView(tplFile.getTimeView());
            newFile.setCreateUserId(tplFile.getCreateUserId());
            newFile.setAddTime(tplFile.getAddTime() != null ? tplFile.getAddTime() : new Timestamp(System.currentTimeMillis()));

            // 插入文件并校验结果
            int fileInsertCount = slOpenNodeFilesMapper.insert(newFile);
            if (fileInsertCount <= 0) {
                throw new RuntimeException("复制节点文件失败，模板节点ID：" + tplFile.getNodeId());
            }
        }

    }

}
