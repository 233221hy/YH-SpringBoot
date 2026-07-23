package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.*;
import cn.xfywz.guozespring.entity.vo.SlTplChapterNode;
import cn.xfywz.guozespring.mapper.CourseMapper;
import cn.xfywz.guozespring.mapper.CourseNodeMapper;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.CourseService;
import cn.xfywz.guozespring.service.admin.SlSchoolService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseNodeMapper courseNodeMapper;
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Override
    public Result AllList(int PageSize, int PageNum) {
        IPage<SlOpenCourse> page = new Page<>(PageNum,PageSize);
        page = courseMapper.AllList(page);
        return Result.success(page.getRecords(),page.getTotal());
    }

    @Override
    public Result selectLikeByName(int PageSize, int PageNum, String name) {
        IPage<SlOpenCourse> page = new Page<>(PageNum,PageSize);
        page = courseMapper.selectLike(page,name);
        return Result.success(page.getRecords());
    }

    @Override
    public Result selectOpenCourseById(int id) {
        return Result.success(courseMapper.getOpenCourse(id));
    }

    @Override
    public Result selectSlTplCourseAll(int PageSize, int PageNum) {
        // 分页查询
        IPage<SlTplCourse> page = new Page<>(PageNum,PageSize);
        page = courseMapper.selectTplCourseList(page);
        return Result.success(page.getRecords(),page.getTotal());
    }

    @Override
    public Result selectLikeSlTplCourse(int PageSize, int PageNum,String name) {
        IPage<SlTplCourse> page = new Page<>(PageNum,PageSize);
        page = courseMapper.selectLikeTplCourse(page,name);
        return Result.success(page.getRecords());
    }

    @Override
    public Result selectSlTplCourseById(int id) {
        return Result.success(courseMapper.selectByIdSlTplCourse(id));
    }

    @Override
    public Result selectSlHomeHotCourse() {
        List<Integer> list = courseMapper.selectHomeHotCourse();
        List<SlTplCourse> list1 = new ArrayList<>();
        SlTplCourse slTplCourse = new SlTplCourse();
        for (Integer integer : list) {
            slTplCourse = courseMapper.selectByIdSlTplCourse(integer);
            list1.add(slTplCourse);
        }
        return Result.success(list1);
    }

    @Override
    public Result selectCourseNode(int id) {
        List<SlOpenChapterNode> slOpenChapterNodes = new ArrayList<>();
        List<SlOpenChapter> list =courseNodeMapper.getChapterName(id);
        for (SlOpenChapter slOpenChapter1 : list ){
            SlOpenChapterNode slOpenChapterNode = new SlOpenChapterNode();
            slOpenChapterNode.setName(slOpenChapter1.getName());
            List<SlOpenNode> slOpenNodes = courseNodeMapper.getCourseNode((int)slOpenChapter1.getId());
            slOpenChapterNode.setSlOpenNodes(slOpenNodes);
            slOpenChapterNodes.add(slOpenChapterNode);
        }
        return Result.success(slOpenChapterNodes);
    }

    @Override
    public Result selectTplNode(int id) {
        List<SlTplChapterNode> slTplChapterNodes = new ArrayList<>();
        List<SlTplChapter> list=courseNodeMapper.getTplChapters(id);
        for (SlTplChapter slTplChapter1 : list ){
            SlTplChapterNode slTplChapterNode = new SlTplChapterNode();
            slTplChapterNode.setId(slTplChapter1.getId());
            slTplChapterNode.setName(slTplChapter1.getName());
            List<SlTplNode> slTplNodes=courseNodeMapper.getTplNodes((int)slTplChapter1.getId());
            slTplChapterNode.setSlTplNodes(slTplNodes);
            slTplChapterNode.setSort(slTplChapter1.getSort());
            slTplChapterNodes.add(slTplChapterNode);
        }
        return Result.success(slTplChapterNodes);
    }

    @Override
    public Result selectTplCourseByCateId(int cateId) {
        return Result.success(courseMapper.selectSlTplCourseByCateBid(cateId));
    }

    @Override
    public Result selectOpenCourseByCateId(int cateId) {
        return Result.success(courseMapper.selectOpenCourseByCateBid(cateId));
    }

    @Override
    public Result selectDomain(String domain) {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("domain",domain);
        SlSchool slSchool = slSchoolMapper.selectOne(queryWrapper);
        return Result.success(slSchool);
    }
}
