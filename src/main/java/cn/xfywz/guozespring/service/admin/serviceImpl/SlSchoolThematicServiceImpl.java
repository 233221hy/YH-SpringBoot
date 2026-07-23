package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.dto.SlSchoolThematicWithCourseNames;
import cn.xfywz.guozespring.entity.dto.TopicWithCoursesDTO;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhmain.SlSchoolThematic;
import cn.xfywz.guozespring.mapper.SlOpenCourseMapper;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.mapper.SlSchoolThematicMapper;
import cn.xfywz.guozespring.service.admin.SlOpenCourseService;
import cn.xfywz.guozespring.service.admin.SlSchoolThematicService;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SlSchoolThematicServiceImpl extends ServiceImpl<SlSchoolThematicMapper, SlSchoolThematic> implements SlSchoolThematicService {

    @Autowired
    private SlOpenCourseService slOpenCourseService;
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private SlOpenCourseMapper slOpenCourseMapper;



    @Override
    public Result selectById(SlSchoolThematic thematic) {
        SlSchoolThematicWithCourseNames dto = new SlSchoolThematicWithCourseNames();
        // 基础字段
        dto.setId(thematic.getId());
        dto.setName(thematic.getName());
        dto.setBackground(thematic.getBackground());
        dto.setTags(thematic.getTags());
        dto.setAllow(thematic.getAllow());
        dto.setSort(thematic.getSort());
        dto.setSchoolId(thematic.getSchoolId());

        // 设置课程名称（仅名称，无ID）
        dto.setCourseName1(getCourseName(thematic.getCourseId1()));
        dto.setCourseName2(getCourseName(thematic.getCourseId2()));
        dto.setCourseName3(getCourseName(thematic.getCourseId3()));
        dto.setCourseName4(getCourseName(thematic.getCourseId4()));
        dto.setCourseName5(getCourseName(thematic.getCourseId5()));
        dto.setCourseName6(getCourseName(thematic.getCourseId6()));

        return Result.success(dto);
    }

    // 根据 courseId 获取课程名称，若不存在则返回 null 或空字符串
    private String getCourseName(Integer courseId) {
        if (courseId == null) {
            return null;
        }
        SlOpenCourse openCourse = slOpenCourseService.selectNameById(courseId);
        if (openCourse != null) {
            return openCourse != null ? openCourse.getName() : null;
        }
        return "";
    }


    /**
     * 根据 domain 查询专题及关联课程
     */
    @Override
    public Result getThematicsWithCoursesByDomain(String domain) {
        // 1. 通过 domain 获取 schoolId，若查不到则使用 0
        int schoolId = 0; // 默认值

        if (domain != null && !domain.trim().isEmpty()) {
            SlSchool school = slSchoolMapper.selectOne(
                    new LambdaQueryWrapper<SlSchool>()
                            .select(SlSchool::getId)
                            .eq(SlSchool::getDomain, domain.trim())
                            .eq(SlSchool::getAllow, (byte) 1)
            );

            if (school != null && school.getId() != null) {
                schoolId = school.getId();
            }
        }

        // 2. 查询该 schoolId 的专题（已审核）
        List<SlSchoolThematic> topics = this.list(
                new LambdaQueryWrapper<SlSchoolThematic>()
                        .eq(SlSchoolThematic::getSchoolId, schoolId)
                        .eq(SlSchoolThematic::getAllow, (byte) 1)
                        .orderByDesc(SlSchoolThematic::getSort)
                        .orderByAsc(SlSchoolThematic::getId)
        );

        if (topics.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 3. 收集所有非空 courseId
        Set<Integer> courseIdSet = new HashSet<>();
        for (SlSchoolThematic t : topics) {
            addIfNotNull(courseIdSet, t.getCourseId1());
            addIfNotNull(courseIdSet, t.getCourseId2());
            addIfNotNull(courseIdSet, t.getCourseId3());
            addIfNotNull(courseIdSet, t.getCourseId4());
            addIfNotNull(courseIdSet, t.getCourseId5());
            addIfNotNull(courseIdSet, t.getCourseId6());
        }

        // 4. 查询课程并构建 map
        Map<Long, SlOpenCourse> courseMap;
        if (!courseIdSet.isEmpty()) {
            Set<Long> courseIdLongSet = courseIdSet.stream()
                    .map(Integer::longValue)
                    .collect(Collectors.toSet());

            LambdaQueryWrapper<SlOpenCourse> queryWrapper = new LambdaQueryWrapper<SlOpenCourse>()
                    .select(SlOpenCourse::getId, SlOpenCourse::getName, SlOpenCourse::getCover,
                            SlOpenCourse::getLecturer, SlOpenCourse::getStuCount, SlOpenCourse::getIntro, SlOpenCourse::getCode)
                    .in(SlOpenCourse::getId, courseIdLongSet);
//                    .eq(SlOpenCourse::getAllow, (byte) 1);

//            if (schoolId > 0) {
//                queryWrapper.eq(SlOpenCourse::getSchoolId, schoolId);
//            }

            List<SlOpenCourse> courses = slOpenCourseMapper.selectList(queryWrapper);
            courseMap = courses.stream()
                    .collect(Collectors.toMap(SlOpenCourse::getId, c -> c));
        } else {
            courseMap = Collections.emptyMap();
        }

        // 5. 组装结果
        List<TopicWithCoursesDTO> result = topics.stream().map(topic -> {
            TopicWithCoursesDTO dto = new TopicWithCoursesDTO();
            dto.setId(topic.getId());
            dto.setName(topic.getName());
            dto.setBackground(topic.getBackground());
            dto.setTags(JsonUtils.parseJsonStringArray(topic.getTags()));

            List<SlOpenCourse> topicCourses = new ArrayList<>();
            addCourseIfExists(topicCourses, courseMap, topic.getCourseId1());
            addCourseIfExists(topicCourses, courseMap, topic.getCourseId2());
            addCourseIfExists(topicCourses, courseMap, topic.getCourseId3());
            addCourseIfExists(topicCourses, courseMap, topic.getCourseId4());
            addCourseIfExists(topicCourses, courseMap, topic.getCourseId5());
            addCourseIfExists(topicCourses, courseMap, topic.getCourseId6());
            dto.setCourses(topicCourses);

            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    // 工具方法（复制过来）
    private void addIfNotNull(Set<Integer> set, Integer id) {
        if (id != null) set.add(id);
    }

    private void addCourseIfExists(List<SlOpenCourse> list, Map<Long, SlOpenCourse> map, Integer courseId) {
        if (courseId != null) {
            SlOpenCourse course = map.get(Long.valueOf(courseId));
            if (course != null) {
                list.add(course);
            }
        }
    }

    // JsonUtils 内部类（复制过来）
    public static class JsonUtils {
        private static final Pattern TAGS_PATTERN = Pattern.compile("\"([^\"]*)\"");

        public static List<String> parseJsonStringArray(String json) {
            if (json == null || json.trim().isEmpty() || "null".equals(json)) {
                return Collections.emptyList();
            }
            String trimmed = json.trim();
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                return Collections.emptyList();
            }
            String content = trimmed.substring(1, trimmed.length() - 1).trim();
            if (content.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>();
            Matcher matcher = TAGS_PATTERN.matcher(content);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
            return result;
        }
    }
}
