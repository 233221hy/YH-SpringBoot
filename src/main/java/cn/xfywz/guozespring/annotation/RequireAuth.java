package cn.xfywz.guozespring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AOP 权限鉴权注解。
 * 标记在 Controller 方法上，自动从请求中提取 schoolId 和 Authorization token 进行校验。
 *
 * <pre>
 * // 用法 1：schoolId 来自 @RequestParam
 * {@code @RequireAuth}
 * {@code @GetMapping("/list")}
 * public Result list(@RequestParam int schoolId, @RequestHeader String Authorization) { ... }
 *
 * // 用法 2：schoolId 来自 DTO 的 getSchoolId()
 * {@code @RequireAuth}
 * {@code @PostMapping("/list")}
 * public Result list(@RequestBody SomeDTO dto, @RequestHeader String Authorization) { ... }
 *
 * // 用法 3：schoolId 来自 Map
 * {@code @RequireAuth}
 * {@code @PostMapping("/list")}
 * public Result list(@RequestBody Map<String, Object> map, @RequestHeader String Authorization) { ... }
 * </pre>
 *
 * 校验失败直接返回 Result.error("非法访问")，不进入方法体。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
}