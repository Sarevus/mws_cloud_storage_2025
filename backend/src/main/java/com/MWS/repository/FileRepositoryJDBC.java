package com.MWS.repository;

import com.MWS.model.File;
import com.MWS.model.UserEntity;
import com.MWS.storage.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FileRepositoryJDBC implements FileRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryJDBC.class);

    @Override
    public File save(File file) {
        String sql = """
            INSERT INTO files (id, user_id, s3_key, original_name, size, mime_type)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ID уже должен быть сгенерирован в конструкторе File
            if (file.getId() == null) {
                throw new IllegalArgumentException("File must have an ID");
            }

            if (file.getUser() == null || file.getUser().getId() == null) {
                throw new IllegalArgumentException("File must have a valid user");
            }

            stmt.setObject(1, file.getId());
            stmt.setObject(2, file.getUser().getId());
            stmt.setString(3, file.getS3Key());
            stmt.setString(4, file.getOriginalName());
            stmt.setLong(5, file.getSize());
            stmt.setString(6, file.getMimeType());

            stmt.executeUpdate();
            logger.info("Файл сохранён: {}", file.getId());
            return file;

        } catch (SQLException e) {
            logger.error("Ошибка сохранения файла с id {}", file.getId(), e);
            throw new RuntimeException("Ошибка сохранения файла", e);
        }
    }

    @Override
    public Optional<File> findById(UUID id) {
        String sql = """
            SELECT f.*, u.id as user_id, u.name, u.email, u.phonenumber, u.password
            FROM files f
            JOIN users u ON f.user_id = u.id
            WHERE f.id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToFile(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Ошибка поиска файла по ID {}", id, e);
            throw new RuntimeException("Ошибка поиска файла по ID", e);
        }
    }

    @Override
    public List<File> findByUserId(UUID userId) {
        String sql = """
            SELECT f.*, u.id as user_id, u.name, u.email, u.phonenumber, u.password
            FROM files f
            JOIN users u ON f.user_id = u.id
            WHERE f.user_id = ?
            ORDER BY f.original_name ASC
            """;

        List<File> files = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                files.add(mapRowToFile(rs));
            }
            logger.info("Найдено {} файлов для пользователя {}", files.size(), userId);
            return files;

        } catch (SQLException e) {
            logger.error("Ошибка поиска файлов пользователя {}", userId, e);
            throw new RuntimeException("Ошибка поиска файлов пользователя", e);
        }
    }

    @Override
    public Optional<File> findByS3Key(String s3Key) {
        String sql = """
            SELECT f.*, u.id as user_id, u.name, u.email, u.phonenumber, u.password
            FROM files f
            JOIN users u ON f.user_id = u.id
            WHERE f.s3_key = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, s3Key);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToFile(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Ошибка поиска файла по S3 ключу {}", s3Key, e);
            throw new RuntimeException("Ошибка поиска файла по S3 ключу", e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM files WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                logger.info("Файл удалён: {}", id);
            } else {
                logger.warn("Файл с id {} не найден для удаления", id);
            }

        } catch (SQLException e) {
            logger.error("Ошибка удаления файла с id {}", id, e);
            throw new RuntimeException("Ошибка удаления файла", e);
        }
    }

    @Override
    public void deleteByS3Key(String s3Key) {
        String sql = "DELETE FROM files WHERE s3_key = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, s3Key);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                logger.info("Файл удалён по S3 ключу: {}", s3Key);
            } else {
                logger.warn("Файл с S3 ключом {} не найден для удаления", s3Key);
            }

        } catch (SQLException e) {
            logger.error("Ошибка удаления файла по S3 ключу {}", s3Key, e);
            throw new RuntimeException("Ошибка удаления файла по S3 ключу", e);
        }
    }

    @Override
    public File update(File file) {
        String sql = """
            UPDATE files 
            SET original_name = ?, size = ?, mime_type = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (file.getId() == null) {
                throw new IllegalArgumentException("File ID cannot be null for update");
            }

            stmt.setString(1, file.getOriginalName());
            stmt.setLong(2, file.getSize());
            stmt.setString(3, file.getMimeType());
            stmt.setObject(4, file.getId());

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                logger.warn("Файл с id {} не найден для обновления", file.getId());
            } else {
                logger.info("Файл обновлён: {}", file.getId());
            }

            return file;

        } catch (SQLException e) {
            logger.error("Ошибка обновления файла с id {}", file.getId(), e);
            throw new RuntimeException("Ошибка обновления файла", e);
        }
    }

    private File mapRowToFile(ResultSet rs) throws SQLException {
        // Создаём пользователя
        UserEntity user = new UserEntity();
        user.setId((UUID) rs.getObject("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phonenumber"));
        user.setPassword(rs.getString("password"));

        // Создаём файл
        File file = new File(
                user,
                rs.getString("original_name"),
                rs.getLong("size"),
                rs.getString("mime_type")
        );

        // Устанавливаем ID и S3 ключ
        file.setId((UUID) rs.getObject("id"));
        file.setS3Key(rs.getString("s3_key"));

        return file;
    }
}