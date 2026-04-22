package com.caresync.dao;

import com.caresync.models.Role;
import com.caresync.models.User;
import com.caresync.utils.Database;
import com.caresync.utils.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    public Optional<User> findByUsername(String username) throws java.sql.SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) throws java.sql.SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws java.sql.SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
        }
        return users;
    }

    public int save(User user) throws java.sql.SQLException {
        if (user.getId() > 0) {
            String sql = """
                    UPDATE users SET full_name=?, username=?, password_hash=COALESCE(NULLIF(?, ''), password_hash),
                    role=?, active=?, password_must_change=? WHERE id=?
                    """;
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.getFullName());
                statement.setString(2, user.getUsername());
                statement.setString(3, user.getPasswordHash());
                statement.setString(4, user.getRole().name());
                statement.setBoolean(5, user.isActive());
                statement.setBoolean(6, user.isPasswordMustChange());
                statement.setInt(7, user.getId());
                statement.executeUpdate();
                return user.getId();
            }
        }

        String sql = "INSERT INTO users(full_name, username, password_hash, role, active, password_must_change) VALUES(?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole().name());
            statement.setBoolean(5, user.isActive());
            statement.setBoolean(6, user.isPasswordMustChange());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user.getId();
        }
    }

    public void delete(int id) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public void updatePassword(int userId, String passwordHash) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET password_hash=?, password_must_change=FALSE WHERE id=?")) {
            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public void setTemporaryPassword(int userId, String passwordHash) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET password_hash=?, password_must_change=TRUE WHERE id=?")) {
            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public int countAll() throws java.sql.SQLException {
        return count("SELECT COUNT(*) FROM users");
    }

    public int countByRole(Role role) throws java.sql.SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int count(String sql) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User map(ResultSet rs) throws java.sql.SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        String roleValue = rs.getString("role");
        user.setRole(Role.valueOf(roleValue == null ? Role.RECEPTIONIST.name() : roleValue));
        user.setActive(rs.getBoolean("active"));
        user.setPasswordMustChange(rs.getBoolean("password_must_change"));
        user.setCreatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        user.setUpdatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("updated_at")));
        return user;
    }
}
