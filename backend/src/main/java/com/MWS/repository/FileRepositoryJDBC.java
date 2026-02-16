package com.MWS.repository;

import com.MWS.model.File;
import com.MWS.model.UserEntity;
import com.MWS.storage.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FileRepositoryJDBC implements FileRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryJDBC.class);

    @Override
    public File save(File file) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // 1. Сохраняем в files (link = s3_key)
            String filesSql = """
                    INSERT INTO files (id, user_id, link, category) 
                    VALUES (?, ?, ?, ?)
                    """;
            try (PreparedStatement filesStmt = conn.prepareStatement(filesSql)) {
                filesStmt.setObject(1, file.getId());
                filesStmt.setObject(2, file.getUser().getId());
                filesStmt.setString(3, file.getS3Key());  // link = s3_key
                filesStmt.setString(4, file.getCategory());
                filesStmt.executeUpdate();
            }

            // 2. Сохраняем в metadata
            String metadataSql = """
                    INSERT INTO metadata 
                    (file_id, original_name, size, mime_type, is_public) 
                    VALUES (?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement metaStmt = conn.prepareStatement(metadataSql)) {
                metaStmt.setObject(1, file.getId());
                metaStmt.setString(2, file.getOriginalName());
                metaStmt.setLong(3, file.getSize());
                metaStmt.setString(4, file.getMimeType());
                metaStmt.setBoolean(5, false);  // is_public по умолчанию
                metaStmt.executeUpdate();
            }

            conn.commit();
            logger.info("Файл и метаданные сохранены: {}", file.getId());
            return file;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rb) {
                }
            }
            logger.error("Ошибка сохранения файла с id {}", file.getId(), e);
            throw new RuntimeException("Ошибка сохранения файла", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public Optional<File> findById(UUID id) {
        String sql = """
                SELECT 
                    f.id, f.user_id, f.link as s3_key, f.category,
                    m.original_name, m.size, m.mime_type, m.is_public,
                    u.name, u.email, u.phone_number, u.password
                FROM files f
                LEFT JOIN metadata m ON f.id = m.file_id
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
                SELECT 
                    f.id, f.user_id, f.link as s3_key, f.category,
                    m.original_name, m.size, m.mime_type, m.is_public,
                    u.name, u.email, u.phone_number, u.password
                FROM files f
                LEFT JOIN metadata m ON f.id = m.file_id
                JOIN users u ON f.user_id = u.id
                WHERE f.user_id = ?
                ORDER BY m.original_name ASC
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
                SELECT 
                    f.id, f.user_id, f.link as s3_key, f.category,
                    m.original_name, m.size, m.mime_type, m.is_public,
                    u.name, u.email, u.phone_number, u.password
                FROM files f
                LEFT JOIN metadata m ON f.id = m.file_id
                JOIN users u ON f.user_id = u.id
                WHERE f.link = ?  -- ищем по link
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
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // 1. Удаляем метаданные
            String deleteMetaSql = "DELETE FROM metadata WHERE file_id = ?";
            try (PreparedStatement metaStmt = conn.prepareStatement(deleteMetaSql)) {
                metaStmt.setObject(1, id);
                metaStmt.executeUpdate();
            }

            // 2. Удаляем файл
            String deleteFileSql = "DELETE FROM files WHERE id = ?";
            try (PreparedStatement fileStmt = conn.prepareStatement(deleteFileSql)) {
                fileStmt.setObject(1, id);
                int affected = fileStmt.executeUpdate();

                if (affected > 0) {
                    logger.info("Файл удалён: {}", id);
                } else {
                    logger.warn("Файл с id {} не найден для удаления", id);
                }
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rb) {
                }
            }
            logger.error("Ошибка удаления файла с id {}", id, e);
            throw new RuntimeException("Ошибка удаления файла", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public void deleteByS3Key(String s3Key) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // 1. Находим ID файла по link (s3_key)
            String findSql = "SELECT id FROM files WHERE link = ?";
            UUID fileId = null;
            try (PreparedStatement findStmt = conn.prepareStatement(findSql)) {
                findStmt.setString(1, s3Key);
                ResultSet rs = findStmt.executeQuery();
                if (rs.next()) {
                    fileId = (UUID) rs.getObject("id");
                }
            }

            if (fileId == null) {
                logger.warn("Файл с S3 ключом {} не найден", s3Key);
                return;
            }

            // 2. Удаляем метаданные
            String deleteMetaSql = "DELETE FROM metadata WHERE file_id = ?";
            try (PreparedStatement metaStmt = conn.prepareStatement(deleteMetaSql)) {
                metaStmt.setObject(1, fileId);
                metaStmt.executeUpdate();
            }

            // 3. Удаляем файл
            String deleteFileSql = "DELETE FROM files WHERE link = ?";
            try (PreparedStatement fileStmt = conn.prepareStatement(deleteFileSql)) {
                fileStmt.setString(1, s3Key);
                int affected = fileStmt.executeUpdate();

                if (affected > 0) {
                    logger.info("Файл удалён по S3 ключу: {}", s3Key);
                }
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rb) {
                }
            }
            logger.error("Ошибка удаления файла по S3 ключу {}", s3Key, e);
            throw new RuntimeException("Ошибка удаления файла по S3 ключу", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public File update(File file) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // 1. Обновляем метаданные
            String updateMetaSql = """
                    UPDATE metadata 
                    SET original_name = ?, size = ?, mime_type = ?
                    WHERE file_id = ?
                    """;
            try (PreparedStatement metaStmt = conn.prepareStatement(updateMetaSql)) {
                metaStmt.setString(1, file.getOriginalName());
                metaStmt.setLong(2, file.getSize());
                metaStmt.setString(3, file.getMimeType());
                metaStmt.setObject(4, file.getId());
                int metaAffected = metaStmt.executeUpdate();

                if (metaAffected == 0) {
                    // Если метаданных нет, создаём их
                    String insertMetaSql = """
                            INSERT INTO metadata 
                            (file_id, original_name, size, mime_type, is_public) 
                            VALUES (?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertMetaSql)) {
                        insertStmt.setObject(1, file.getId());
                        insertStmt.setString(2, file.getOriginalName());
                        insertStmt.setLong(3, file.getSize());
                        insertStmt.setString(4, file.getMimeType());
                        insertStmt.setBoolean(5, false);
                        insertStmt.executeUpdate();
                    }
                }
            }

            // 2. Обновляем файл (link и category)
            String updateFileSql = """
                    UPDATE files 
                    SET link = ?, category = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement fileStmt = conn.prepareStatement(updateFileSql)) {
                fileStmt.setString(1, file.getS3Key());  // link = s3_key
                fileStmt.setString(2, file.getCategory());
                fileStmt.setObject(3, file.getId());
                fileStmt.executeUpdate();
            }

            conn.commit();
            logger.info("Файл обновлён: {}", file.getId());
            return file;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rb) {
                }
            }
            logger.error("Ошибка обновления файла с id {}", file.getId(), e);
            throw new RuntimeException("Ошибка обновления файла", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public List<File> findByUserIdAndCategory(UUID userId, String category) {
        String sql = """
                SELECT 
                    f.id, f.user_id, f.link as s3_key, f.category,
                    m.original_name, m.size, m.mime_type, m.is_public,
                    u.name, u.email, u.phone_number, u.password
                FROM files f
                LEFT JOIN metadata m ON f.id = m.file_id
                JOIN users u ON f.user_id = u.id
                WHERE f.user_id = ? AND f.category = ?
                ORDER BY m.original_name ASC
                """;

        List<File> files = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            stmt.setString(2, category);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                files.add(mapRowToFile(rs));
            }
            return files;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> findDistinctCategoriesByUser(UUID userId) {
        String sql = """
                SELECT DISTINCT category 
                FROM files 
                WHERE user_id = ? 
                ORDER BY category
                """;

        List<String> categories = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
            return categories;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private File mapRowToFile(ResultSet rs) throws SQLException {
        UserEntity user = new UserEntity();
        user.setId((UUID) rs.getObject("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setPassword(rs.getString("password"));

        // Все поля читаются правильно благодаря алиасам в SQL
        File file = new File(
                user,
                rs.getString("original_name"),  // из metadata
                rs.getLong("size"),             // из metadata
                rs.getString("mime_type"),      // из metadata
                rs.getString("category")        // из files
        );

        file.setId((UUID) rs.getObject("id"));
        file.setS3Key(rs.getString("s3_key"));  // из files.link (алиас s3_key)

        return file;
    }
}