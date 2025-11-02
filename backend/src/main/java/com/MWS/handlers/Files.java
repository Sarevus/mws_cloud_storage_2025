package com.MWS.handlers;

import com.MWS.security.CustomUserDetails; // кастомный класс для информации о пользователе
import com.MWS.service.FileService; // кастомный сервис для работы с файлами
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import java.util.List;

@RestController //помечает класс как Spring MVC контроллер, где каждый метод возвращает данные (JSON/XML) вместо имени view
@RequestMapping("/files") //задает базовый URL для всех методов класса: http://ваш-сервер/files
public class Files {
    private final FileService fileService;

    public Files(FileService fileService) {
        this.fileService = fileService;
    }
/*
    @GetMapping("/user/{userId}") // по этому адресу выводятся все файлы доступные пользователю
    public List<String> getUserFiles(@PathVariable Long userId) {
        return fileService.getFileLinksByUserId(userId); // возвращает JSON список ссылок
    }
*/
//времянка
    @GetMapping("/user")  // ← УБИРАЕМ {userId} из пути
    public List<String> getUserFiles() {
        // ВРЕМЕННО: используем фиксированный userId
        Long userId = 1L;
        return fileService.getFileLinksByUserId(userId);
    }


    @PostMapping("/upload")
    // принимает файл, вызывает метод чтобы его записать, возращает ссылку на файл и код 200
    public ResponseEntity<String> uploadFile(//@AuthenticationPrincipal CustomUserDetails customUser, убрал временно, чтобы не мучиться пока с уетентификацией
                                             @RequestParam("file") MultipartFile file) // сам файл. Тут MultipartFile поможет в случае чего получить ориг название файла, его тип и размер
    {
        Long userId = 1L;//времянка
        String fileUrl = fileService.saveUserFile(userId, file);//времянка
        //String fileUrl = fileService.saveUserFile(customUser.getUserId(), file); // ссылка на файл вида "http://yourserver.com/files/" + fileName, полученная из fileService !!!тоже пока убрал
        return ResponseEntity.ok(fileUrl); // Вернем код, что все ок и ссылку на файл
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<Void> deleteFile(//@AuthenticationPrincipal CustomUserDetails customUser, !!!временно убрал
                                           @PathVariable String fileId) {
        // Вызов сервиса для удаления файла с проверкой userId
        // !!! времянка
        Long userId = 1L;
        fileService.deleteUserFile(/*customUser.getU*/userId/*()*/, fileId);
        return ResponseEntity.noContent().build(); // Возвращаем 204 No Content
    }


}
