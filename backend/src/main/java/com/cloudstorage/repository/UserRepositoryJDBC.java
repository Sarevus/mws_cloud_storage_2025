package com.cloudstorage.repository;

import com.cloudstorage.model.User;
import com.cloudstorage.storage.Database;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class UserRepositoryJDBC implements UserRepository {

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (id, name, email, phone_number, password) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Если у пользователя нет ID - генерируем
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }

            stmt.setObject(1, user.getId());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhoneNumber());
            stmt.setString(5, user.getPassword());

            stmt.executeUpdate();
            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения пользователя", e);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        String sql = "SELECT id, name, email, phone_number, password FROM users WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, name, email, phone_number, password FROM users WHERE email = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по email", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки email", e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления пользователя", e);
        }
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, phone_number = ?, password = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhoneNumber());
            stmt.setString(4, user.getPassword());
            stmt.setObject(5, user.getId());

            stmt.executeUpdate();
            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления пользователя", e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId((UUID) rs.getObject("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setPassword(rs.getString("password"));
        return user;
    }
}