package com.MWS.db.postgresql;

import com.MWS.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    // Используем Config для получения настроек
    private static final String URL = Config.getDatabaseUrl();
    private static final String USER = Config.getDatabaseUser();
    private static final String PASSWORD = Config.getDatabasePassword();

    static {
        // Загрузка драйвера PostgreSQL при инициализации класса
        try {
            Class.forName(Config.getDatabaseDriver());
            logger.info("PostgreSQL драйвер успешно загружен");
        } catch (ClassNotFoundException e) {
            logger.error("Не удалось загрузить PostgreSQL драйвер", e);
            throw new RuntimeException("PostgreSQL драйвер не найден", e);
        }
    }

    /**
     * Получает новое подключение к базе данных
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            logger.debug("Установлено соединение с БД");
            return conn;
        } catch (SQLException e) {
            logger.error("Ошибка подключения к БД: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Проверяет подключение к базе данных
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn.isValid(5); // Таймаут 5 секунд
            if (isValid) {
                logger.info("Подключение к БД успешно");
            } else {
                logger.warn("Подключение к БД недействительно");
            }
            return isValid;
        } catch (SQLException e) {
            logger.error("Не удалось подключиться к БД", e);
            return false;
        }
    }
}