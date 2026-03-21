package com.MWS.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VerificationCodeService {

    private final Map<String, Integer> codes = new HashMap<>();

    public void saveCode(String email, int code) {
        codes.put(email, code);
    }

    public boolean verifyCode(String email, int code) {
        Integer savedCode = codes.get(email);
        return savedCode == code;
    }

    public void removeCode(String email) {
        codes.remove(email);
    }
}