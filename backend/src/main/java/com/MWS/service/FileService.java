package com.MWS.service;

import com.MWS.dto.create_update.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileService {


    public List<FileDto> getFileLinksByUserId(UUID userId); // я отдаю user id должна получить список ссылок на файлы доступные пользовавтелю

    public String saveUserFile(UUID userId, MultipartFile file);

    void deleteUserFile(UUID userId);

}
