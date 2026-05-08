package com.caresync.dao;

import com.caresync.models.Receptionist;
import com.caresync.utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReceptionistDAO {
    public List<Receptionist> findAll() throws java.sql.SQLException {
        String sql = """
                SELECT r.*, u.full_name
                FROM receptionists r
                JOIN users u ON u.id = r.user_id
                ORDER BY u.full_name
                """;
        List<Receptionist> receptionists = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                receptionists.add(map(rs));
            }
        }
        return receptionists;
    }

    public Optional<Receptionist> findByUserId(int userId) throws java.sql.SQLException {
        String sql = """
                SELECT r.*, u.full_name
                FROM receptionists r
                JOIN users u ON u.id = r.user_id
                WHERE r.user_id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int save(Receptionist receptionist) throws java.sql.SQLException {
        Optional<Receptionist> existing = findByUserId(receptionist.getUserId());
        if (receptionist.getId() <= 0 && existing.isPresent()) {
            receptionist.setId(existing.get().getId());
        }
        if (receptionist.getId() > 0) {
            String sql = "UPDATE receptionists SET phone=?, email=?, shift=? WHERE id=?";
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, receptionist.getPhone());
                statement.setString(2, receptionist.getEmail());
                statement.setString(3, receptionist.getShift());
                statement.setInt(4, receptionist.getId());
                statement.executeUpdate();
                return receptionist.getId();
            }
        }

        String sql = "INSERT INTO receptionists(user_id, phone, email, shift) VALUES(?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, receptionist.getUserId());
            statement.setString(2, receptionist.getPhone());
            statement.setString(3, receptionist.getEmail());
            statement.setString(4, receptionist.getShift());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    receptionist.setId(keys.getInt(1));
                }
            }
            return receptionist.getId();
        }
    }

    public int countAll() throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM receptionists");
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void deleteByUserId(int userId) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM receptionists WHERE user_id=?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private Receptionist map(ResultSet rs) throws java.sql.SQLException {
        Receptionist receptionist = new Receptionist();
        receptionist.setId(rs.getInt("id"));
        receptionist.setUserId(rs.getInt("user_id"));
        receptionist.setFullName(rs.getString("full_name"));
        receptionist.setPhone(rs.getString("phone"));
        receptionist.setEmail(rs.getString("email"));
        receptionist.setShift(rs.getString("shift"));
        return receptionist;
    }
}
