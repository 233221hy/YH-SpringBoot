package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.entity.mhsch.YeeCourse;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CourseRowMapper {

    public static YeeCourse fromRow(ResultSet rs) throws SQLException {
        YeeCourse c = new YeeCourse();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setMode(rs.getLong("mode"));
        c.setCollegeId(rs.getLong("collegeId"));
        c.setCategoryId(rs.getString("categoryId"));
        c.setLecturers(rs.getString("lecturers"));
        c.setStartDate(rs.getDate("startDate"));
        c.setEndDate(rs.getDate("endDate"));
        c.setCover(rs.getString("cover"));
        c.setContent(rs.getString("content"));
        c.setCredit(rs.getDouble("credit"));
        c.setAllow(rs.getLong("allow"));
        c.setIntro(rs.getString("intro"));
        c.setTeacherIntro(rs.getString("teacherIntro"));
        c.setCode(rs.getString("code"));
        c.setStuCount(rs.getLong("stuCount"));
        c.setProclamation(rs.getString("proclamation"));
        c.setClusterId(rs.getLong("clusterId"));
        c.setPeriodName(rs.getString("periodName"));
        c.setAddTime(rs.getTimestamp("addTime"));
        c.setCreateId(rs.getLong("createId"));
        c.setSchoolId(rs.getLong("schoolId"));
        c.setCateBid(rs.getLong("cateBid"));
        c.setCateMid(rs.getLong("cateMid"));
        c.setSignStartTime(rs.getTimestamp("signStartTime"));
        c.setSignEndTime(rs.getTimestamp("signEndTime"));
        c.setSignScope(rs.getLong("signScope"));
        c.setSignClass(rs.getString("signClass"));
        c.setLecturerName(rs.getString("lecturerName"));
        c.setOffline(rs.getLong("offline"));
        c.setMission(rs.getLong("mission"));
        c.setSignLimit(rs.getLong("signLimit"));
        c.setLineLock(rs.getLong("lineLock"));
        c.setAddDate(rs.getDate("addDate"));
        c.setTplId(rs.getLong("tplId"));
        c.setIsPractice(rs.getLong("isPractice"));
        return c;
    }
}
