package com.MWS.repository;

import com.MWS.dto.UserStorageDtoInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserStorageRepositoryJpa implements UserStorageRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public Optional<UserStorageDtoInfo> getUserStorage(UUID userId) {
        String sql = "SELECT total_bytes FROM user_storage_usage WHERE user_id = ?";

        try {
            Long bytes = jdbcTemplate.queryForObject(sql, Long.class, userId);
            return Optional.of(new UserStorageDtoInfo(userId, bytes));
        } catch (Exception e) {
            return Optional.of(new UserStorageDtoInfo(userId, 0));
        }
    }
}
