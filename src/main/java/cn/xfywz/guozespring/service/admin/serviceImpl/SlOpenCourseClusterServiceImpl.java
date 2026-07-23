package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourseCluster;
import cn.xfywz.guozespring.entity.vo.CourseCluster;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.mapper.SlOpenCourseClusterMapper;
import cn.xfywz.guozespring.mapper.SlOpenCourseMapper;
import cn.xfywz.guozespring.service.admin.SlOpenCourseClusterService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SlOpenCourseClusterServiceImpl implements SlOpenCourseClusterService {
    @Autowired
    private SlOpenCourseClusterMapper slOpenCourseClusterMapper;
    @Autowired
    private SlOpenCourseMapper slOpenCourseMapper;
    @Override
    public Result selectAll(int pageSize, int pageNum, TplCourseLike condition) {
        List<CourseCluster> courseClusters = new ArrayList<>();

        // 构建分页对象
        Page<SlOpenCourseCluster> page = new Page<>(pageNum, pageSize);

        // 构建 SlOpenCourseCluster 的查询条件
        QueryWrapper<SlOpenCourseCluster> clusterQuery = new QueryWrapper<>();

        if (condition != null) {
            // schoolId：精确匹配（假设 >0 才有效）
            if (condition.getSchoolId() != null && condition.getSchoolId() > 0) {
                clusterQuery.eq("schoolId", condition.getSchoolId());
            }

            // name：模糊查询
            if (StringUtils.isNotBlank(condition.getName())) {
                clusterQuery.like("name", condition.getName());
            }

            // code：模糊查询
            if (StringUtils.isNotBlank(condition.getCode())) {
                clusterQuery.like("code", condition.getCode());
            }

            // cateBid：模糊 or 精确？你原先是 like，这里保持 like
            if (condition.getCateBid() != null) {
                clusterQuery.like("cateBid", condition.getCateBid());
            }

            // cateMid：同上
            if (condition.getCateMid() != null) {
                clusterQuery.like("cateMid", condition.getCateMid());
            }
        }

        clusterQuery.orderByDesc("addTime");

        // 分页查询集群
        Page<SlOpenCourseCluster> clusterPage = slOpenCourseClusterMapper.selectPage(page, clusterQuery);

        // 遍历结果，补充关联课程（无额外条件）
        for (SlOpenCourseCluster clusterRecord : clusterPage.getRecords()) {
            CourseCluster courseCluster = new CourseCluster();
            // 复制属性（可考虑用 BeanUtils 或构造器简化）
            courseCluster.setId(clusterRecord.getId());
            courseCluster.setName(clusterRecord.getName());
            courseCluster.setEnName(clusterRecord.getEnName());
            courseCluster.setCode(clusterRecord.getCode());
            courseCluster.setSchoolId(clusterRecord.getSchoolId());
            courseCluster.setCateBid(clusterRecord.getCateBid());
            courseCluster.setCateMid(clusterRecord.getCateMid());
            courseCluster.setCover(clusterRecord.getCover());
            courseCluster.setCreateId(clusterRecord.getCreateId());
            courseCluster.setAddTime(clusterRecord.getAddTime());

            // 查询该集群下的所有课程（不再加 condition 中的条件！）
            QueryWrapper<SlOpenCourse> courseQuery = new QueryWrapper<>();
            courseQuery.eq("clusterId", clusterRecord.getId()); // 注意字段名是否为 clusterId

            List<SlOpenCourse> courses = slOpenCourseMapper.selectList(courseQuery);
            courseCluster.setSlOpenCourse(courses);

            courseClusters.add(courseCluster);
        }

        return Result.success(courseClusters, clusterPage.getTotal());
    }

    @Override
    public Result add(SlOpenCourseCluster slOpenCourseCluster) {
        slOpenCourseCluster.setAddTime(new java.sql.Timestamp(System.currentTimeMillis()));
        int insert = slOpenCourseClusterMapper.insert(slOpenCourseCluster);
        return insert > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    @Override
    public Result del(int id) {
        int delete = slOpenCourseClusterMapper.deleteById(id);
        return delete > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Override
    public Result update(SlOpenCourseCluster slOpenCourseCluster) {
        int update = slOpenCourseClusterMapper.updateById(slOpenCourseCluster);
        return update > 0 ? Result.success("修改成功") : Result.error("修改失败");
    }

}
