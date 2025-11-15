//package com.MWS.controller;
//
//import com.MWS.model.FileDto;
//import com.MWS.service.FileService;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/files")
//public class FileController {
//
//    private final FileService fileService;
//
//    public FileController(FileService fileService) {
//        this.fileService = fileService;
//    }
//
//    @PostMapping("/user/{userId}")
//    public String uploadFile(@PathVariable UUID userId, @RequestParam MultipartFile file) {
//        return fileService.saveUserFile(userId, file);
//    }
//
//    @GetMapping("/user/{userId}")
//    public List<FileDto> getUserFiles(@PathVariable UUID userId) {
//        return fileService.getFileLinksByUserId(userId);
//    }
//
//    @DeleteMapping("/{fileId}")
//    public void deleteFile(@PathVariable UUID fileId) {
//        fileService.deleteUserFile(fileId);
//    }
//}