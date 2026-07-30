package cn.xfywz.guozespring.util;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.SlSchoolService;
import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 学校域名解析工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchoolDomainResolver {

    private final SlSchoolMapper slSchoolMapper;

    // Caffeine缓存配置
    private final Cache<String, Integer> domainSchoolIdCache = Caffeine.newBuilder()
            .maximumSize(1000)                     // 最大缓存条目数
            .expireAfterWrite(30, TimeUnit.MINUTES) // 写入后30分钟过期
            .recordStats()                         // 开启统计功能
            .removalListener((key, value, cause) ->
                    log.debug("缓存移除: key={}, value={}, cause={}", key, value, cause))
            .build();

    /**
     * 通过HttpServletRequest获取schoolId
     * - 若域名未绑定学校，返回 0（主站）
     */
    public int getSchoolIdByHost(HttpServletRequest request) {
        String domain = request.getHeader("Host");
        domain = "yit.haiqikeji.com";
        if (StringUtils.isBlank(domain)) {
            throw new BusinessException("请求域名缺失");
        }

        return domainSchoolIdCache.get(domain, this::loadSchoolIdFromDb);
    }

    /**
     * 从数据库加载 schoolId（供缓存调用）
     */
    private Integer loadSchoolIdFromDb(String domain) {
        QueryWrapper<SlSchool> qw = new QueryWrapper<>();
        qw.eq("domain", domain).eq("allow", 1); // 只查已审核的学校
        SlSchool school = slSchoolMapper.selectOne(qw);

        if (school != null) {
            log.debug("域名 {} 匹配学校 ID: {}", domain, school.getId());
            return school.getId();
        }

        // 未找到：缓存 0（主站），避免缓存穿透
        return 0;
    }




}
