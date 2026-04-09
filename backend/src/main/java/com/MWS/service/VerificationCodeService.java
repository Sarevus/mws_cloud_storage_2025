package com.MWS.service;

import com.MWS.dto.create_update.CreateUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final long CODE_EXPIRATION_MINUTES = 5;

    private final Map<String, CreateUserDTO> pendingRegistrations = new ConcurrentHashMap<>();

    public void savePendingUser(CreateUserDTO request, int code) {
        pendingRegistrations.put(request.email(), request);
        redisTemplate.opsForValue().set(request.email(), String.valueOf(code), 5, TimeUnit.MINUTES);
    }

    public CreateUserDTO getPendingUser(String email) {
        return pendingRegistrations.get(email);
    }

    public void saveCode(String email, int code) {
        redisTemplate.opsForValue().set(email, String.valueOf(code),
                CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    public boolean verifyCode(String email, int code) {
        String savedCode = redisTemplate.opsForValue().get(email);
        return savedCode != null && savedCode.equals(String.valueOf(code));
    }

    public void removeCode(String email) {
        redisTemplate.delete(email);
        pendingRegistrations.remove(email);
    }
}