package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.dto.YeeSchoolColumnDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeSchoolColumn;
import cn.xfywz.guozespring.service.teacher.YeeSchoolColumnService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
public class YeeSchoolColumnController {

    @Autowired
    private YeeSchoolColumnService yeeSchoolColumnService;

    @PostMapping("/column_list")
    public Result getColumnList(@RequestBody YeeSchoolColumnDTO yeeSchoolColumnDTO,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeSchoolColumnDTO.getSchoolId())) {
            return yeeSchoolColumnService.getColumnList(yeeSchoolColumnDTO);
        } else return Result.error("非法访问");
    }

    @PostMapping("/column_add")
    public Result addColumn(@RequestBody YeeSchoolColumn yeeSchoolColumn,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, yeeSchoolColumn.getSchoolId())) {
            return yeeSchoolColumnService.add(yeeSchoolColumn);
        } else return Result.error("非法访问");
    }

    @PostMapping("/column_update")
    public Result updateColumn(@RequestBody YeeSchoolColumn yeeSchoolColumn,
                               @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, yeeSchoolColumn.getSchoolId())) {
            return yeeSchoolColumnService.update(yeeSchoolColumn);
        } else return Result.error("非法访问");
    }

    // 新增：根据ID查询，供新增/编辑页面加载具体内容
    @PostMapping("/column_get")
    public Result getColumn(@RequestBody YeeSchoolColumnDTO param,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) param.getSchoolId())){
            return yeeSchoolColumnService.getById(param);
        } else return Result.error("非法访问");
    }

    @PostMapping("/column_delete")
    public Result deleteColumn(@RequestBody YeeSchoolColumnDTO param,
                               @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) param.getSchoolId())){
            return yeeSchoolColumnService.delete(param);
        } else return Result.error("非法访问");
    }
}
