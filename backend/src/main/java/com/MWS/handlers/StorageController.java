package com.MWS.handlers;

import com.MWS.dto.UserStorageDtoInfo;
import com.MWS.service.UserStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
public class StorageController {

    @Autowired
    UserStorageService userStorageService;

    @GetMapping("/info")
    public UserStorageService.StorageInfo getStorageInfo(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("No such user");
        }

        return userStorageService.getStorageInfo(userId);
    }
}
