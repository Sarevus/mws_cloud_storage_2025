package com.MWS.service;

import java.util.List;

public interface FileService {


    public List<String> getFileLinksByUserId(long userId); // я отдаю user id должна получить список ссылок на файлы доступные пользовавтелю

    public String saveUserFile(Long userId, Object file);

    void deleteUserFile(Long userId, String fileId);

}
