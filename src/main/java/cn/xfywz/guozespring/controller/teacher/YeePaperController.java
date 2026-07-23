package cn.xfywz.guozespring.controller.teacher;


import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.mhsch.YeePaper;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.service.teacher.YeePaperService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.xml.crypto.Data;

/**
 * @Author: ChengLin
 * 试卷 yee_paper
 */
@RestController
@RequestMapping("/school")
public class YeePaperController {

    @Autowired
    private YeePaperService yeePaperService;
    @GetMapping("/yee_paper_list")
    public Result selectAll(@RequestParam Integer schoolId,
                            @RequestParam(required = false) Integer userId,
                            @RequestParam(required = false) String title,
                            @RequestParam(required = false) Integer type,
                            @RequestParam(required = false) Integer allow,
                            @RequestParam(required = false) Integer cateBid,
                            @RequestParam(required = false) Integer cateMid,
                            @RequestParam(required = false) Integer pageNum,
                            @RequestParam(required = false) Integer pageSize,
                            @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            Integer dataAuth = AuthTokenUtil.extractDataAuth(Authorization);
            if (dataAuth.equals(DataAuth.OWN.getValue())){
                return yeePaperService.selectAll(schoolId, userId, title, type, allow, cateBid, cateMid, pageNum, pageSize);
            } else {
                return yeePaperService.selectAll(schoolId, null, title, type, allow, cateBid, cateMid, pageNum, pageSize);
            }

        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_paper_add")
    public Result add(@RequestBody YeePaper yeePaper,
                      @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeePaper.getSchoolId())){
            return yeePaperService.add(yeePaper);
        }else return Result.error("非法访问");
    }

    @PostMapping("/yee_paper_addBlank")
    public Result addBlank(@RequestBody YeePaper yeePaper,
                      @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeePaper.getSchoolId())){
            return yeePaperService.addBlank(yeePaper);
        }else return Result.error("非法访问");
    }

    @PostMapping("/yee_paper_update")
    public Result update(@RequestBody YeePaper yeePaper,
                      @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeePaper.getSchoolId())){
            return yeePaperService.update(yeePaper);
        }else return Result.error("非法访问");
    }

    @PostMapping("/yee_paper_delete")
    public Result delete(@RequestParam int schoolId,
                         @RequestParam int id,
                         @RequestParam Integer userId,
                         @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperService.delete(schoolId, id, userId);
        }else return Result.error("非法访问");
    }

    @PostMapping("/yee_paper_allow")
    public Result allow(@RequestParam int schoolId,
                         @RequestParam int id,
                         @RequestParam Integer userId,
                         @RequestParam Byte allow,
                         @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperService.allow(schoolId, id, userId, allow);
        }else return Result.error("非法访问");
    }

    @GetMapping("/yee_paper_getById")
    public Result getById(@RequestParam int schoolId,
                         @RequestParam int id,
                         @RequestParam Integer userId,
                         @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperService.getById(schoolId, id, userId);
        }else return Result.error("非法访问");
    }

    @PostMapping("/yee_paper_changeTeacher")
    public Result changeTeacher(@RequestParam int schoolId,
                         @RequestParam int id,
                         @RequestParam Integer userId,
                         @RequestParam Integer teacherId,
                         @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperService.changeTeacher(schoolId, id, userId, teacherId);
        }else return Result.error("非法访问");
    }

}
