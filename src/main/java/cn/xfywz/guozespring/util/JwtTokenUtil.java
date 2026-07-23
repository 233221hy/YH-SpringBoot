package cn.xfywz.guozespring.util;

import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JwtTokenUtil {
    private static final String key = "guoze-spring-@123guoze-spring-@123";
    /** JWT 过期时间：24小时（秒），与 Redis token TTL 保持一致 */
    public static final long TOKEN_EXPIRE_SECONDS = 60 * 60 * 24;
    private static final long expire = TOKEN_EXPIRE_SECONDS;
    /** Redis key 前缀，格式：jwt:token:{account}:{jti} */
    public static final String TOKEN_REDIS_KEY_PREFIX = "jwt:token:";
    /** Redis key 模糊匹配前缀，用于清理某账号的所有旧 token */
    public static final String TOKEN_REDIS_KEY_PATTERN = TOKEN_REDIS_KEY_PREFIX + "%s:*";
//    private static final SecretKey secretKey = Jwts.SIG.HS256.key().random(new SecureRandom(key.getBytes(StandardCharsets.UTF_8))).build();
    private static final SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));


    public static String getToken(LoginUser loginUser) throws Exception{
        Map<String,Object> claims = new HashMap<>();
        String msg= JSON.toJSONString(loginUser);
        //"iss" (Issuer): 代表 JWT 的签发者。在这个字段中填入一个字符串，表示该 JWT 是由谁签发的。例如，可以填入你的应用程序的名称或标识符。
        claims.put("iss","guoze");
        //"sub" (Subject): 代表 JWT 的主题，即该 JWT 所面向的用户。可以是用户的唯一标识符或者其他相关信息。
        claims.put("sub",msg);
        claims.put("role",loginUser.getAuthorities());
        //"iat" (Issued At): 代表 JWT 的签发时间。同样使用 UNIX 时间戳表示。
        claims.put("iat",new Date());
        //"jti" (JWT ID): JWT 的唯一标识符。这个字段可以用来标识 JWT 的唯一性，避免重放攻击等问题。
        claims.put("jti", UUID.randomUUID().toString());
        return Jwts.builder()
                .claims()
                .add(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expire * 1000)) // 从当前时间开始计算过期时间
                .and()
                .signWith(secretKey)
                .compact();
    }

    public static String getToken(YeeStudent yeeStudent) throws Exception {
        Map<String, Object> claims = new HashMap<>();

        // 把整个 yeeStudent 转成 JSON 字符串放进 sub
        String studentJson = JSON.toJSONString(yeeStudent); // 使用 FastJSON2
        claims.put("sub", studentJson);

        claims.put("iss", "guoze");
        claims.put("iat", new Date());
        claims.put("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .claims()
                .add(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expire * 1000)) // 从当前时间开始计算过期时间
                .and()
                .signWith(secretKey)
                .compact();
    }

    public static Claims parseToken(String token) throws Exception{
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parse(token).accept(Jws.CLAIMS)
                .getPayload();
    }

    /** 从 JWT token 中提取 jti（JWT ID），用于构造 Redis key */
    public static String extractJti(String token) throws Exception {
        Claims claims = parseToken(token);
        return claims.get("jti", String.class);
    }

    /** 构建 Redis token key：jwt:token:{account}:{jti} */
    public static String buildTokenRedisKey(String account, String jti) {
        return TOKEN_REDIS_KEY_PREFIX + account + ":" + jti;
    }

    /** JWT 剩余有效时间（秒），负数或 null 表示已过期 */
    public static long getRemainingSeconds(Claims claims) {
        java.util.Date expiration = claims.getExpiration();
        if (expiration == null) return -1;
        return (expiration.getTime() - System.currentTimeMillis()) / 1000;
    }

    /** 自动续签阈值：剩余不足 1 小时时颁发新 token */
    public static final long REFRESH_THRESHOLD_SECONDS = 3600;
}