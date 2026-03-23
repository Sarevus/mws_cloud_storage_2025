package com.MWS.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(".")  // Ищет .env в корне проекта
            .ignoreIfMissing()
            .load();

    // ==================== Ceph/S3 Configuration ====================

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

    // ==================== Database Configuration ====================

    public static String getDatabaseUrl() {
        return dotenv.get("DB_URL", "jdbc:postgresql://localhost:5432/cloud3_data");
    }

    public static String getDatabaseUser() {
        return dotenv.get("DB_USER", "developer");
    }

    public static String getDatabasePassword() {
        return dotenv.get("DB_PASSWORD", "dev123");
    }

    public static String getDatabaseDriver() {
        return dotenv.get("DB_DRIVER", "org.postgresql.Driver");
    }

    // ==================== Server Configuration ====================

    public static int getServerPort() {
        return Integer.parseInt(dotenv.get("SERVER_PORT", "6969"));
    }

    public static long getMaxFileSize() {
        return Long.parseLong(dotenv.get("MAX_FILE_SIZE_MB", "100")) * 1024 * 1024;
    }

    public static String[] getAllowedFileTypes() {
        String types = dotenv.get("ALLOWED_FILE_TYPES",
                "image/jpeg,image/png,image/gif,application/pdf,text/plain");
        return types.split(",");
    }

    // ==================== Application Configuration ====================

    public static String getAppName() {
        return dotenv.get("APP_NAME", "Cloud Storage Service");
    }

    public static String getAppVersion() {
        return dotenv.get("APP_VERSION", "1.0.0");
    }

    public static boolean isDebugMode() {
        return Boolean.parseBoolean(dotenv.get("DEBUG_MODE", "false"));
    }

    // ==================== Utility Methods ====================

    /**
     * Проверяет, загружены ли все критические настройки
     */
    public static boolean validateConfiguration() {
        try {
            getCephEndpoint();
            getCephAccessKey();
            getCephSecretKey();
            getCephBucketName();
            getDatabaseUrl();
            getDatabaseUser();
            getDatabasePassword();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Выводит конфигурацию (без паролей) для отладки
     */
    public static void printConfiguration() {
        System.out.println("=== Configuration ===");
        System.out.println("App Name: " + getAppName());
        System.out.println("App Version: " + getAppVersion());
        System.out.println("Server Port: " + getServerPort());
        System.out.println("Max File Size: " + (getMaxFileSize() / 1024 / 1024) + " MB");
        System.out.println("Ceph Endpoint: " + getCephEndpoint());
        System.out.println("Ceph Bucket: " + getCephBucketName());
        System.out.println("Database URL: " + getDatabaseUrl());
        System.out.println("Debug Mode: " + isDebugMode());
        System.out.println("====================");
    }

    // Dotenv - библиотека, которая загружает переменные окружения из .env
    // По умолчанию хост, на котором запущен Redis, это localhost
    public static String getRedisHost() {
        return dotenv.get("REDIS_HOST", "localhost");
    }

    public static int getRedisPort() {
        return Integer.parseInt(dotenv.get("REDIS_PORT", "6379"));
    }
}