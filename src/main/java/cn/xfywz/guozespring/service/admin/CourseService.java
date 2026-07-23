package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface CourseService {
    //公开课
    Result AllList(int PageSize, int PageNum);
    //模糊查询
    Result selectLikeByName(int PageSize ,int PageNum,String name);
    //id查询
    Result selectOpenCourseById(int id);

    //全部课程
    Result selectSlTplCourseAll(int PageSize,int PageNum);
    //模糊查询
    Result selectLikeSlTplCourse(int PageSize,int PageNum,String name);
    //id查询
    Result selectSlTplCourseById(int id);

    //热门课程
    Result selectSlHomeHotCourse();

    //获取公开课程章节
    Result selectCourseNode(int id);
    //获取全部课程章节
    Result selectTplNode(int id);

    //分类id查询全部课程
    Result selectTplCourseByCateId(int cateId);

    //分类id查询开放课程
    Result selectOpenCourseByCateId(int cateId);

    Result selectDomain(String domain);
}
