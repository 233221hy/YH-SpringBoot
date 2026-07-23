package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.YeeSchoolColumnDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeSchoolColumn;
import cn.xfywz.guozespring.util.Result;

public interface YeeSchoolColumnService {
    Result getColumnList(YeeSchoolColumnDTO yeeSchoolColumnDTO);

    Result add(YeeSchoolColumn yeeSchoolColumn);

    Result update(YeeSchoolColumn yeeSchoolColumn);

    // 新增：根据ID查询栏目详情，用于新增/编辑页面展示具体内容
    Result getById(YeeSchoolColumnDTO param);

    Result delete(YeeSchoolColumnDTO param);
}
