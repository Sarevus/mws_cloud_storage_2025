package com.MWS.repository;

import com.MWS.model.File;
import com.MWS.model.UserEntity;
import com.MWS.storage.Database;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FileRepositoryJDBC implements FileRepository {

    @Override
    public File save(File file) {
        String sql = """
            INSERT INTO files (id, user_id, s3_key, original_name, size, mime_type, is_public, description, uploaded_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ID уже должен быть сгенерирован в конструкторе File
            if (file.getId() == null) {
                throw new IllegalArgumentException("File must have an ID");
            }

            stmt.setObject(1, file.getId());
            stmt.setObject(2, file.getUser().getId());
            stmt.setString(3, file.getS3Key());
            stmt.setString(4, file.getOriginalName());
            stmt.setLong(5, file.getSize());
            stmt.setString(6, file.getMimeType());
            stmt.setBoolean(7, file.getIsPublic());
            stmt.setString(8, file.getDescription());
            stmt.setTimestamp(9, Timestamp.valueOf(file.getUploadedAt()));
            stmt.setTimestamp(10, Timestamp.valueOf(file.getUpdatedAt()));

            stmt.executeUpdate();
            return file;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения файла", e);
        }
    }

    @Override
    public Optional<File> findById(UUID id) {
        String sql = """
            SELECT f.*, u.id as user_id, u.name, u.email, u.phone_number, u.password
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
            throw new RuntimeException("Ошибка поиска файла по ID", e);
        }
    }

    @Override
    public List<File> findByUserId(UUID userId) {
        String sql = """
            SELECT f.*, u.id as user_id, u.name, u.email, u.phone_number, u.password
            FROM files f
            JOIN users u ON f.user_id = u.id
            WHERE f.user_id = ?
            ORDER BY f.uploaded_at DESC
            """;

        List<File> files = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                files.add(mapRowToFile(rs));
            }
            return files;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска файлов пользователя", e);
        }
    }

    @Override
    public InputStream findByS3Key(String s3Key) {
        String sql = """
            SELECT f.*, u.id as user_id, u.name, u.email, u.phone_number, u.password
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
            throw new RuntimeException("Ошибка поиска файла по S3 ключу", e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM files WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления файла", e);
        }
    }

    @Override
    public File update(File file) {
        String sql = """
            UPDATE files 
            SET original_name = ?, size = ?, mime_type = ?, is_public = ?, 
                description = ?, updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, file.getOriginalName());
            stmt.setLong(2, file.getSize());
            stmt.setString(3, file.getMimeType());
            stmt.setBoolean(4, file.getIsPublic());
            stmt.setString(5, file.getDescription());
            stmt.setTimestamp(6, Timestamp.valueOf(file.getUpdatedAt()));
            stmt.setObject(7, file.getId());

            stmt.executeUpdate();
            return file;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления файла", e);
        }
    }

    private File mapRowToFile(ResultSet rs) throws SQLException {
        // Создаём пользователя
        User user = new User();
        user.setId((UUID) rs.getObject("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setPassword(rs.getString("password"));

        // Создаём файл
        File file = new File(
                user,
                rs.getString("original_name"),
                rs.getLong("size"),
                rs.getString("mime_type")
        );

        // Устанавливаем дополнительные поля
        file.setId((UUID) rs.getObject("id"));
        file.setS3Key(rs.getString("s3_key"));
        file.setIsPublic(rs.getBoolean("is_public"));
        file.setDescription(rs.getString("description"));

        // Устанавливаем даты
        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        if (uploadedAt != null) {
            file.setUploadedAt(uploadedAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            file.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return file;
    }
}