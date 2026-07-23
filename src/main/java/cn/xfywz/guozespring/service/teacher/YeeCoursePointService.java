package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;

public interface YeeCoursePointService {
    Result selectAll(int schoolId, Integer courseId, String title, Integer classId) throws Exception;

    Result selectTodayAll(int schoolId, Integer courseId, String title, Integer classId, String date) throws  Exception;

    Result selectMonthAll(int schoolId, Integer courseId, String title, Integer classId, String date) throws  Exception;

    // 导出：全部数据
    void exportAll(int schoolId, Integer courseId, String title, Integer classId, HttpServletResponse response) throws Exception;
    // 导出：今日数据（或指定日期）
    void exportToday(int schoolId, Integer courseId, String title, Integer classId, String date, HttpServletResponse response) throws Exception;
    // 导出：本月数据（或指定月份）
    void exportMonth(int schoolId, Integer courseId, String title, Integer classId, String date, HttpServletResponse response) throws Exception;
}
