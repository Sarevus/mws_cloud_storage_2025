package com.MWS.service;

import java.util.HashMap;
import java.util.Map;

public class CategoryDetector {
    private static final Map<String, String> MIME_TYPE_CATEGORIES = new HashMap<>();
    private static final Map<String, String> EXTENSION_CATEGORIES = new HashMap<>();

    static {
        MIME_TYPE_CATEGORIES.put("image/", "photos");
        MIME_TYPE_CATEGORIES.put("video/", "videos");
        MIME_TYPE_CATEGORIES.put("audio/", "music");
        MIME_TYPE_CATEGORIES.put("application/pdf", "documents");
        MIME_TYPE_CATEGORIES.put("text/", "documents");
        MIME_TYPE_CATEGORIES.put("application/msword", "documents");
        MIME_TYPE_CATEGORIES.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "documents");
        MIME_TYPE_CATEGORIES.put("application/vnd.ms-excel", "documents");
        MIME_TYPE_CATEGORIES.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "documents");
        MIME_TYPE_CATEGORIES.put("application/vnd.ms-powerpoint", "documents");
        MIME_TYPE_CATEGORIES.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "documents");

        // Extension → фронтенд-категории
        EXTENSION_CATEGORIES.put("jpg", "photos");
        EXTENSION_CATEGORIES.put("jpeg", "photos");
        EXTENSION_CATEGORIES.put("png", "photos");
        EXTENSION_CATEGORIES.put("gif", "photos");
        EXTENSION_CATEGORIES.put("bmp", "photos");
        EXTENSION_CATEGORIES.put("svg", "photos");
        EXTENSION_CATEGORIES.put("webp", "photos");

        EXTENSION_CATEGORIES.put("mp4", "videos");
        EXTENSION_CATEGORIES.put("avi", "videos");
        EXTENSION_CATEGORIES.put("mov", "videos");
        EXTENSION_CATEGORIES.put("mkv", "videos");
        EXTENSION_CATEGORIES.put("webm", "videos");
        EXTENSION_CATEGORIES.put("wmv", "videos");

        EXTENSION_CATEGORIES.put("mp3", "music");
        EXTENSION_CATEGORIES.put("wav", "music");
        EXTENSION_CATEGORIES.put("flac", "music");
        EXTENSION_CATEGORIES.put("aac", "music");
        EXTENSION_CATEGORIES.put("ogg", "music");
        EXTENSION_CATEGORIES.put("m4a", "music");

        EXTENSION_CATEGORIES.put("pdf", "documents");
        EXTENSION_CATEGORIES.put("doc", "documents");
        EXTENSION_CATEGORIES.put("docx", "documents");
        EXTENSION_CATEGORIES.put("xls", "documents");
        EXTENSION_CATEGORIES.put("xlsx", "documents");
        EXTENSION_CATEGORIES.put("ppt", "documents");
        EXTENSION_CATEGORIES.put("pptx", "documents");
        EXTENSION_CATEGORIES.put("txt", "documents");
        EXTENSION_CATEGORIES.put("md", "documents");
        EXTENSION_CATEGORIES.put("rtf", "documents");
    }

    public static String detectCategory(String mimeType, String fileName) {
        // 1. По MIME-type
        if (mimeType != null) {
            for (Map.Entry<String, String> entry : MIME_TYPE_CATEGORIES.entrySet()) {
                if (mimeType.startsWith(entry.getKey()) || mimeType.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        // 2. По расширению файла (fallback)
        if (fileName != null) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                String extension = fileName.substring(lastDot + 1).toLowerCase();
                if (EXTENSION_CATEGORIES.containsKey(extension)) {
                    return EXTENSION_CATEGORIES.get(extension);
                }
            }
        }

        // 3. По умолчанию
        return "general";
    }
}