package com.MWS.service;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.List;

public interface UserService {

    void save(String userName, String email, String phoneNumber, String password);


}
