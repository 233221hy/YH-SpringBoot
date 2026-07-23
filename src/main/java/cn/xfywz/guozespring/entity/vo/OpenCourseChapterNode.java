package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.entity.mhmain.SlOpenChapter;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import cn.xfywz.guozespring.entity.mhmain.SlOpenNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenCourseChapterNode extends SlOpenCourse {
    List<SlOpenChapter> slOpenChapters;
}
