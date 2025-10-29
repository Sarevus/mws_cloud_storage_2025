package com.MWS.handlers;

import com.MWS.security.CustomUserDetails;
import com.MWS.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import java.util.List;

@RestController
@RequestMapping("/files")
public class Files {
    private final FileService fileService;

    public Files(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/user/{userId}") // по этому адресу выводятся все файлы доступные пользователю
    public List<String> getUserFiles(@PathVariable Long userId) {
        return fileService.getFileLinksByUserId(userId); // возвращает JSON список ссылок
    }

    @PostMapping("/upload")
    // принимает файл, вызывает метод чтобы его записать, возращает ссылку на файл и код 200
    public ResponseEntity<String> uploadFile(@AuthenticationPrincipal CustomUserDetails customUser,
                                             @RequestParam("file") MultipartFile file) // сам файл. Тут MultipartFile поможет в случае чего получить ориг название файла, его тип и размер
    {
        String fileUrl = fileService.saveUserFile(customUser.getUserId(), file); // ссылка на файл вида "http://yourserver.com/files/" + fileName, полученная из fileService
        return ResponseEntity.ok(fileUrl); // Вернем код, что все ок и ссылку на файл
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<Void> deleteFile(@AuthenticationPrincipal CustomUserDetails customUser,
                                           @PathVariable String fileId) {
        // Вызов сервиса для удаления файла с проверкой userId
        fileService.deleteUserFile(customUser.getUserId(), fileId);
        return ResponseEntity.noContent().build(); // Возвращаем 204 No Content
    }


}
