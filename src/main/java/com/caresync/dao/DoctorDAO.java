package com.caresync.dao;

import com.caresync.models.Doctor;
import com.caresync.utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDAO {
    public List<Doctor> findAll() throws java.sql.SQLException {
        String sql = """
                SELECT d.*, u.full_name
                FROM doctors d
                JOIN users u ON u.id = d.user_id
                ORDER BY u.full_name
                """;
        List<Doctor> doctors = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                doctors.add(map(rs));
            }
        }
        return doctors;
    }

    public Optional<Doctor> findByUserId(int userId) throws java.sql.SQLException {
        String sql = """
                SELECT d.*, u.full_name
                FROM doctors d
                JOIN users u ON u.id = d.user_id
                WHERE d.user_id = ?
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

    public Optional<Doctor> findById(int id) throws java.sql.SQLException {
        String sql = """
                SELECT d.*, u.full_name
                FROM doctors d
                JOIN users u ON u.id = d.user_id
                WHERE d.id = ?
                """;
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

    public int save(Doctor doctor) throws java.sql.SQLException {
        Optional<Doctor> existing = findByUserId(doctor.getUserId());
        if (doctor.getId() <= 0 && existing.isPresent()) {
            doctor.setId(existing.get().getId());
        }
        if (doctor.getId() > 0) {
            String sql = "UPDATE doctors SET specialization=?, phone=?, email=?, room=?, availability=? WHERE id=?";
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                fill(statement, doctor);
                statement.setInt(6, doctor.getId());
                statement.executeUpdate();
                return doctor.getId();
            }
        }

        String sql = "INSERT INTO doctors(user_id, specialization, phone, email, room, availability) VALUES(?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, doctor.getUserId());
            statement.setString(2, doctor.getSpecialization());
            statement.setString(3, doctor.getPhone());
            statement.setString(4, doctor.getEmail());
            statement.setString(5, doctor.getRoom());
            statement.setString(6, doctor.getAvailability());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    doctor.setId(keys.getInt(1));
                }
            }
            return doctor.getId();
        }
    }

    public int countAll() throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM doctors");
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean hasClinicalDataForUser(int userId) throws java.sql.SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM doctors d
                LEFT JOIN appointments a ON a.doctor_id = d.id
                LEFT JOIN medical_history mh ON mh.doctor_id = d.id
                WHERE d.user_id = ?
                  AND (a.id IS NOT NULL OR mh.id IS NOT NULL)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void deleteByUserId(int userId) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM doctors WHERE user_id=?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private void fill(PreparedStatement statement, Doctor doctor) throws java.sql.SQLException {
        statement.setString(1, doctor.getSpecialization());
        statement.setString(2, doctor.getPhone());
        statement.setString(3, doctor.getEmail());
        statement.setString(4, doctor.getRoom());
        statement.setString(5, doctor.getAvailability());
    }

    private Doctor map(ResultSet rs) throws java.sql.SQLException {
        Doctor doctor = new Doctor();
        doctor.setId(rs.getInt("id"));
        doctor.setUserId(rs.getInt("user_id"));
        doctor.setFullName(rs.getString("full_name"));
        doctor.setSpecialization(rs.getString("specialization"));
        doctor.setPhone(rs.getString("phone"));
        doctor.setEmail(rs.getString("email"));
        doctor.setRoom(rs.getString("room"));
        doctor.setAvailability(rs.getString("availability"));
        return doctor;
    }
}
