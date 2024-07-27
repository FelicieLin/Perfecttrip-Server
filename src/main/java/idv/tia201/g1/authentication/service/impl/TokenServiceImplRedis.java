package idv.tia201.g1.authentication.service.impl;

import com.google.gson.Gson;
import idv.tia201.g1.authentication.service.TokenService;
import idv.tia201.g1.authentication.service.UserAuth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static idv.tia201.g1.utils.Constants.LOGIN_TTL;
import static idv.tia201.g1.utils.Constants.LOGIN_USER;

@Service
public class TokenServiceImplRedis implements TokenService {
    private final Gson gson = new Gson();
    private final StringRedisTemplate stringRedisTemplate;

    public TokenServiceImplRedis(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public <T extends UserAuth> String setToken(T user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = LOGIN_USER + token;

        String json = gson.toJson(new TokenData(user, user.getClass().getName()));
        stringRedisTemplate.opsForValue().set(key, json, LOGIN_TTL, TimeUnit.SECONDS);
        return token;
    }

    @Override
    public UserAuth validateToken(String token) {
        String key = LOGIN_USER + token;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            TokenData tokenData = gson.fromJson(json, TokenData.class);
            try {
                Class<?> userClass = Class.forName(tokenData.getType());
                return (UserAuth) gson.fromJson(gson.toJson(tokenData.getData()), userClass);
            } catch (ClassNotFoundException e) {
                // TODO: 日誌紀錄
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void flashLoginExpire(String token) {
        String key = LOGIN_USER + token;
        stringRedisTemplate.expire(key, LOGIN_TTL, TimeUnit.SECONDS);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TokenData {
        private Object data;
        private String type;
    }
}
