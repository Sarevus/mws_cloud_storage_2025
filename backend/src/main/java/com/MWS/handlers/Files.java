package com.MWS.handlers;

import static spark.Spark.*;

import com.MWS.service.FileService;
import com.MWS.service.FileServiceRelease;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.List;

public class Files {
    private static final FileService fileService = new FileServiceRelease();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Object getList(spark.Request request, spark.Response response) throws JsonProcessingException {
        String userIdStr = request.queryParams("userId");

        if (userIdStr == null){
            response.status(400);

            return "не верные данные";
        }

        Long userId = Long.parseLong(userIdStr);
        List<String> files = fileService.getFileLinksByUserId(userId);

        response.type("application/json");

        return objectMapper.writeValueAsString(files); // Вернет ["file1","file2"]
    }

    public static Object downloadFile(spark.Request request, spark.Response response){

        return "<li><ol>file1</ol><ol>file2</ol><ol>file1</ol><ol>file3</ol><ol>file1</ol></li>";
    }

    public static Object uploadFile(spark.Request request, spark.Response response){

        return "<li><ol>file1</ol><ol>file2</ol><ol>file1</ol><ol>file3</ol><ol>file1</ol></li>";
    }

    public static Object deleteFile(spark.Request request, spark.Response response){

        return "<li><ol>file1</ol><ol>file2</ol><ol>file1</ol><ol>file3</ol><ol>file1</ol></li>";
    }
}
