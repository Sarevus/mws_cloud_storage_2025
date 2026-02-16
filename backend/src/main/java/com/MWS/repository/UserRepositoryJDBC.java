package com.MWS.repository;

import com.MWS.storage.Database;
import com.MWS.model.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryJDBC implements UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryJDBC.class);

    @Override
    public UserEntity save(UserEntity user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        String name = user.getName();
        String email = user.getEmail();
        String phone_number = user.getPhoneNumber();
        String password = user.getPassword();

        String sql = "INSERT INTO users (id, name, email, phone_number, password) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setObject(1, user.getId());
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone_number);
            stmt.setString(5, password);
            stmt.executeUpdate();
            logger.info("Пользователь сохранён: {}", user.getId());
            return user;

        } catch (SQLException e) {
            logger.error("Ошибка при сохранении пользователя с id {}", user.getId(), e);
            throw new RuntimeException("Не удалось сохранить пользователя", e);
        }
    }

    @Override
    public UserEntity update(UserEntity user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("ID пользователя не существует");
        }
        String name = user.getName();
        String email = user.getEmail();
        String phone_number = user.getPhoneNumber();
        String password = user.getPassword();
        UUID id = user.getId();

        String sql = "UPDATE users SET name = ?, email = ?, phone_number = ?, password = ? WHERE id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

//            stmt.setObject(1, user.getId());
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, phone_number);
            stmt.setString(4, password);
            stmt.setObject(5, id);
            stmt.executeUpdate();
            logger.info("Пользователь обновлён: {}", user.getId());

        } catch (SQLException e) {
            logger.error("Ошибка при сохранении пользователя с id {}", user.getId(), e);
            throw new RuntimeException("Не удалось обновить пользователя", e);
        }
        return user;
    }

    @Override
    public Optional<UserEntity> findById(UUID id) {
        String sql = "SELECT id, name, email, phone_number, password FROM users WHERE id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserEntity user = mapRowToUser(rs);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске пользователя с id {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setObject(1, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                logger.info("Пользователь удалён: {}", id);
            } else {
                logger.info("Пользователь с id {} не найден для удаления", id);
            }

        } catch (SQLException e) {
            logger.error("Ошибка при удалении пользователя с id {}", id, e);
            throw new RuntimeException("Не удалось удалить пользователя с id: " + id, e);
        }
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        String sql = "SELECT id, name, email, phone_number, password FROM users WHERE email = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserEntity user = mapRowToUser(rs);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске пользователя по email {}", email, e);
        }
        return Optional.empty();
    }



    private UserEntity mapRowToUser(ResultSet rs) throws SQLException {
        UserEntity user = new UserEntity();
        user.setId((UUID) rs.getObject("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setPassword(rs.getString("password"));
        return user;
    }
}
