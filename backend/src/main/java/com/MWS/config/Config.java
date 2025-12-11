package com.MWS.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(".")  // Ищет .env в корне проекта
            .ignoreIfMissing()
            .load();

    // Ceph
    public static String getCephEndpoint() {
        return dotenv.get("CEPH_ENDPOINT", "http://localhost:9000");
    }

    public static String getCephAccessKey() {
        return dotenv.get("CEPH_ACCESS_KEY", "MY_CUSTOM_ACCESS_KEY");
    }

    public static String getCephSecretKey() {
        return dotenv.get("CEPH_SECRET_KEY", "MY_CUSTOM_SECRET_KEY");
    }

    public static String getCephBucketName() {
        return dotenv.get("CEPH_BUCKET_NAME", "cloudstorage");
    }

    // Server
    public static int getServerPort() {
        return Integer.parseInt(dotenv.get("SERVER_PORT", "8080"));
    }

    public static long getMaxFileSize() {
        return Long.parseLong(dotenv.get("MAX_FILE_SIZE_MB", "100")) * 1024 * 1024;
    }
}