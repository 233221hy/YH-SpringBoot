package cn.xfywz.guozespring.mapper;

import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import cn.xfywz.guozespring.entity.mhmain.SlTplCourse;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper {
    //公开课
//    @DS("mhsch_1")
//    @Select("select * from sl_open_course where allow=1")
//    @Select("select * from yee_course where allow=1")
//    IPage<SlOpenCourse> AllList(IPage<SlOpenCourse> page);

    // 公开课（补上排序）
    @Select("SELECT * FROM sl_open_course WHERE allow = 1 ORDER BY addTime DESC")
    IPage<SlOpenCourse> AllList(IPage<SlOpenCourse> page);

    //查询
    @Select("select * from sl_open_course where name like #{name} and allow=1 ORDER BY addTime DESC")
    IPage<SlOpenCourse> selectLike(IPage<SlOpenCourse> page,String name);

    //id查询
    @Select("select * from sl_open_course where id=#{id}")
    SlOpenCourse getOpenCourse(int id);



    // 全部课程
    @Select("""
    select * from sl_tpl_course
    where allow = 1
    order by
        case when weight > 0 then 1 else 2 end,
        case when weight > 0 then weight else 0 end desc,
        addTime desc
    """)
    IPage<SlTplCourse> selectTplCourseList(IPage<SlTplCourse> page);

    // 模糊查询全部课程
    @Select("SELECT * FROM sl_tpl_course WHERE name LIKE #{name} AND allow = 1 ORDER BY addTime DESC")
    IPage<SlTplCourse> selectLikeTplCourse(IPage<SlTplCourse> page, String name);

    //id查询
    @Select("select * from sl_tpl_course where id=#{id}")
    SlTplCourse selectByIdSlTplCourse(int id);

    //首页热门课程
    @Select("select courseId from sl_home_hot_course where allow=1 ORDER BY addTime DESC")
    List<Integer> selectHomeHotCourse();

    //分类id查询公开课
    @Select("select * from sl_open_course where cateBid=#{cateBid} and allow=1 ORDER BY addTime DESC")
    List<SlOpenCourse> selectOpenCourseByCateBid(int cateBid);

    //分类id查询全部课程
    @Select("select * from sl_tpl_course where cateBid=#{cateBid} and allow=1 ORDER BY addTime DESC")
    List<SlTplCourse> selectSlTplCourseByCateBid(int cateBid);



}
