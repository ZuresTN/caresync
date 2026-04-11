package com.caresync.dao;

import com.caresync.utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDAO {
    public Map<String, String> findAll() throws java.sql.SQLException {
        Map<String, String> settings = new LinkedHashMap<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM system_settings ORDER BY setting_key");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        return settings;
    }

    public String findValue(String key, String fallback) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT setting_value FROM system_settings WHERE setting_key=?")) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString("setting_value") : fallback;
            }
        }
    }

    public void save(String key, String value) throws java.sql.SQLException {
        String sql = """
                INSERT INTO system_settings(setting_key, setting_value)
                VALUES(?,?)
                ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }
}
