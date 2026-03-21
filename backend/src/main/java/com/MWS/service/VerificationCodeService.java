package com.MWS.service;

import com.MWS.dto.create_update.CreateUserDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private final Map<String, CreateUserDTO> pendingRegistrations = new ConcurrentHashMap<>();
    private final Map<String, Integer> codes = new ConcurrentHashMap<>();

    public void savePendingUser(CreateUserDTO request, int code) {
        pendingRegistrations.put(request.email(), request);
        codes.put(request.email(), code);
    }

    public CreateUserDTO getPendingUser(String email) {
        return pendingRegistrations.get(email);
    }
    public void saveCode(String email, int code) {
        codes.put(email, code);
    }

    public boolean verifyCode(String email, int code) {
        return codes.containsKey(email) && codes.get(email) == code;
    }

    public void removeCode(String email) {
        codes.remove(email);
        pendingRegistrations.remove(email);
    }
}