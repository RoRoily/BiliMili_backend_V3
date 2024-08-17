package com.bilimili.buaa13.tools;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static javax.crypto.Cipher.SECRET_KEY;

@Component
@Slf4j
public class JsonWebTokenTool {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 有效期2天，记得修改 UserAccountServiceImpl 的 login 中redis的时间，注意单位，这里是毫秒
    private static final String JsonWebToken_KEY = "CqcF+Jka+C+CFiOMTMz16JsT+/jzNVETa2g9iTvOi/TwpKR3R0YGUVoHjB6j6HDwYgTdbvnl4NKZ/j+6BSmEKg==";
    private static final long JsonWebToken_Validity = 1000L * 60 * 60 * 24 * 2;
    private static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获取token密钥
     * @return 加密后的token密钥
     */
    private static SecretKey getSecretToken() {
        byte[] encodeKey = Base64.getDecoder().decode(JsonWebTokenTool.JsonWebToken_KEY);
        return new SecretKeySpec(encodeKey, 0, encodeKey.length, "HmacSHA256");
    }

    /**
     * 生成token
     * @param uid 用户id
     * @param role 用户角色 user/admin
     * @return token
     */
    public String createToken(String uid, String role) {
        String uuid = getUUID();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        SecretKey secretKey = getSecretToken();
        long currentMillis = System.currentTimeMillis();
        Date current = new Date(currentMillis);
        long expirationMillis = currentMillis + JsonWebTokenTool.JsonWebToken_Validity;
        Date expirationDate = new Date(expirationMillis);

        String token = Jwts.builder()
                .setId(uuid)    // 随机id，用于生成无规则token
                .setSubject(uid)    // 加密主体
                .claim("role", role)    // token角色参数 user/admin 用于区分普通用户和管理员
                .signWith(secretKey, signatureAlgorithm)
                .setIssuedAt(current)
                .setExpiration(expirationDate)
                .compact();

        try {
            //缓存token信息，管理员和用户之间不要冲突
            //使用 指定有效期 和 指定时间单位 存储简单数据类型
            redisTemplate.opsForValue().set("token:" + role + ":" + uid, token, JsonWebTokenTool.JsonWebToken_Validity, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("存储redis数据异常", e);
        }
        return token;
    }

    /**
     * 获取Claims信息
     * @param token token
     * @return token的claims
     */
    private static Claims getAllClaimsFromToken(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretToken())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException eje) {
            claims = null;
            log.error("获取token信息异常，jwt已过期");
        } catch (Exception e) {
            claims = null;
            log.error("获取token信息失败", e);
        }
        return claims;
    }

    /**
     * 获取token的uid，uid就是主题
     * @param token token
     * @return uid的字符串类型
     */
    public static String getUidFromToken(String token) {
        String uid = null;
        try {
            Claims claims = getAllClaimsFromToken(token);
            if (claims != null) {
                uid = claims.getSubject();
            }
        } catch (Exception e) {
            log.error("从token里获取不到主题", e);
        }
        return uid;
    }

    /**
     * 在token里获取对应参数的值
     * @param token token
     * @param param 参数名
     * @return 参数值
     */
    public static String getClaimFromToken(String token, String param) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims == null || !claims.containsKey(param)) {
            return "";
        }
        return claims.get(param).toString();
    }

    /**
     * 校验传送来的token和缓存的token是否一致
     * @param token token
     * @return true/false
     */
    public boolean verifyToken(String token) {
        if (token == null || StringUtils.isEmpty(token)) { return false;}
        Claims claims = getAllClaimsFromToken(token);
        if (claims == null) {
            return false;
        }
        String uid = getUidFromToken(token);
        String role = getClaimFromToken(token,"role");
        String cacheToken;
        try {
            cacheToken = String.valueOf(redisTemplate.opsForValue().get("token:" + role + ":" + uid));
        } catch (Exception e) {
            log.error("获取不到缓存的token", e);
            return false;
        }
        return token.equals(cacheToken);
    }

    public static void main(String[] args) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[64]; // 256-bit key
        secureRandom.nextBytes(keyBytes);
        String jwtKey = Base64.getEncoder().encodeToString(keyBytes);
        System.out.println("Generated JWT Key: " + jwtKey);
    }

    private void UUIDTest(){
        // 生成一个随机 UUID
        UUID randomUUID = UUID.randomUUID();
        System.out.println("Random UUID: " + randomUUID);
        // 根据字节数组生成 UUID
        byte[] bytes = "example".getBytes();
        UUID nameUUID = UUID.nameUUIDFromBytes(bytes);
        System.out.println("Name-based UUID: " + nameUUID);
        // 获取 UUID 的各部分
        String uuidString = randomUUID.toString();
        String[] parts = uuidString.split("-");
        System.out.println("Part 1: " + parts[0]); // 8 characters
        System.out.println("Part 2: " + parts[1]); // 4 characters
        System.out.println("Part 3: " + parts[2]); // 4 characters
        System.out.println("Part 4: " + parts[3]); // 4 characters
        System.out.println("Part 5: " + parts[4]); // 12 characters
    }

    private static SecretKey getSecretKey() {
        byte[] decodedKey = Base64.getDecoder().decode(String.valueOf(SECRET_KEY));
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
    }

    public static boolean checkToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims != null;
        } catch (ExpiredJwtException e) {
            System.out.println("Token expired: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("Token invalid: " + e.getMessage());
            return false;
        }
    }

    private static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.out.println("Failed to parse token: " + e.getMessage());
            return null;
        }
    }

    public static String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return (claims != null) ? claims.getSubject() : null;
    }

    public static boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

}
