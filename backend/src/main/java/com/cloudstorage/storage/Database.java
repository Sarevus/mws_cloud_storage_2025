package com.cloudstorage.storage;

import com.cloudstorage.config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.getDatabaseUrl(),
                Config.getDatabaseUser(),
                Config.getDatabasePassword()
        );
    }
}