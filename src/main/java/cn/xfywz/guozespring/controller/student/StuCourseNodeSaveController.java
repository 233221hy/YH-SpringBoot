package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.entity.dto.*;
import cn.xfywz.guozespring.service.student.YeeStudyTotalService;
import cn.xfywz.guozespring.service.student.serviceImpl.YeeStudyTotalServiceImpl;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.GetOutIpUtil;
import cn.xfywz.guozespring.util.IpUtils;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import static org.apache.commons.collections4.MapUtils.getInteger;

import java.util.Map;


@RestController
@RequestMapping("/user")
public class StuCourseNodeSaveController {

    @Autowired
    private YeeStudyTotalService yeeStudyTotalService;
    @Autowired
    private StringRedisTemplate redisTemplate;


    @GetMapping("/get_study_progress")
    public Result getStudyProgress(
            @RequestParam Integer schoolId,
            @RequestParam Integer userId,
            @RequestParam Integer courseId,
            @RequestHeader String Authorization) throws Exception {
        if (schoolId == null || userId == null || courseId == null) {
            return Result.error("参数不能为空");
        }
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法请求");
        }
        return yeeStudyTotalService.getStudyProgress(schoolId, userId, courseId);
    }

    @PostMapping("/study_session_start")
    public Result startSession(@RequestBody StudySessionStartDTO dto,
                               @RequestHeader String Authorization,
                               HttpServletRequest request) throws Exception{
        if (!AuthTokenUtil.verifyToken(Authorization, dto.getSchoolId())) {
            return Result.error("非法请求");
        }
        // 自动提取真实 IP
        String realIp = IpUtils.getClientIp(request);
        return yeeStudyTotalService.startSession(dto,realIp);
    }

    @PostMapping("/study_session_heartbeat")
    public Result heartbeat(@RequestBody StudySessionHeartbeatDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Result.error("sessionId 不能为空");
        }

        // 直接使用Service里定义好的常量前缀 study_session:，不再硬编码
        String hashKey = YeeStudyTotalServiceImpl.SESSION_HASH_KEY_PREFIX + sessionId;
        if (!redisTemplate.hasKey(hashKey)) {
            return Result.error("会话不存在或已过期");
        }

        // 删掉所有Token校验相关代码、入参、throws Exception
        return yeeStudyTotalService.heartbeat(dto);
    }

    @PostMapping("/study_session_end")
    public Result endSession(@RequestBody StudySessionEndDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Result.error("sessionId 不能为空");
        }

        String hashKey = YeeStudyTotalServiceImpl.SESSION_HASH_KEY_PREFIX + sessionId;
        Map<Object, Object> sessionMap = redisTemplate.opsForHash().entries(hashKey);
        if (sessionMap == null || sessionMap.isEmpty()) {
            return Result.error("会话不存在或已过期");
        }

        // 移除Authorization header、移除token校验逻辑
        return yeeStudyTotalService.endSession(dto);
    }

    @GetMapping("/last_progress")
    public Result getLastProgress(
            @RequestParam Integer nodeId,
            @RequestParam Integer userId,
            @RequestParam Integer schoolId,
            @RequestHeader String Authorization) throws Exception{
        if (nodeId == null || userId == null || schoolId == null) {
            return Result.error("参数不能为空");
        }
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法请求");
        }
        return yeeStudyTotalService.getLastProgressStr(userId, nodeId, schoolId);
    }


//    @PostMapping("/save_study_record")
//    public Result saveStudyRecord(Integer schoolId, Integer userId, Integer nodeId, Integer courseId, Integer duration, @RequestHeader String Authorization) throws Exception {
//        if (schoolId == null || userId == null || nodeId == null || courseId == null || duration == null) {
//            return Result.error("参数不能为空");
//        }
//        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
//            String ip = GetOutIpUtil.getOutIp();
//            return yeeStudyTotalService.saveStudyRecord(schoolId,userId,nodeId,courseId,duration,"0",ip,null);
//        }else return Result.error("非法请求");
//    }

}