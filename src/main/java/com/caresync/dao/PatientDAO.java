package com.caresync.dao;

import com.caresync.models.Patient;
import com.caresync.models.Gender;
import com.caresync.utils.Database;
import com.caresync.utils.DateTimeUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientDAO {
    public List<Patient> findAll(String search) throws java.sql.SQLException {
        String filter = search == null ? "" : search.trim();
        String sql = """
                SELECT * FROM patients
                WHERE ? = ''
                   OR first_name LIKE ?
                   OR last_name LIKE ?
                   OR phone LIKE ?
                   OR email LIKE ?
                ORDER BY created_at DESC
                """;
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + filter + "%";
            statement.setString(1, filter);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, like);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    patients.add(map(rs));
                }
            }
        }
        return patients;
    }

    public List<Patient> findByDoctorUserId(int doctorUserId, String search) throws java.sql.SQLException {
        String filter = search == null ? "" : search.trim();
        String sql = """
                SELECT DISTINCT p.*
                FROM patients p
                LEFT JOIN appointments a ON a.patient_id = p.id
                LEFT JOIN medical_history mh ON mh.patient_id = p.id
                JOIN doctors d ON d.id = a.doctor_id OR d.id = mh.doctor_id
                WHERE d.user_id = ?
                  AND (? = ''
                       OR p.first_name LIKE ?
                       OR p.last_name LIKE ?
                       OR p.phone LIKE ?
                       OR p.email LIKE ?)
                ORDER BY p.created_at DESC
                """;
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + filter + "%";
            statement.setInt(1, doctorUserId);
            statement.setString(2, filter);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, like);
            statement.setString(6, like);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    patients.add(map(rs));
                }
            }
        }
        return patients;
    }

    public Optional<Patient> findById(int id) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM patients WHERE id=?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Patient> findByUserId(int userId) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM patients WHERE user_id=?")) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int save(Patient patient) throws java.sql.SQLException {
        if (patient.getId() > 0) {
            String sql = """
                    UPDATE patients SET user_id=?, first_name=?, last_name=?, gender=?, date_of_birth=?, phone=?, email=?,
                    address=?, blood_group=?, allergies=?, emergency_contact=? WHERE id=?
                    """;
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                fill(statement, patient);
                statement.setInt(12, patient.getId());
                statement.executeUpdate();
                return patient.getId();
            }
        }

        String sql = """
                INSERT INTO patients(user_id, first_name, last_name, gender, date_of_birth, phone, email, address, blood_group, allergies, emergency_contact)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(statement, patient);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    patient.setId(keys.getInt(1));
                }
            }
            return patient.getId();
        }
    }

    public void delete(int id) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM patients WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public int countAll() throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM patients");
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean hasClinicalDataForUser(int userId) throws java.sql.SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM patients p
                LEFT JOIN appointments a ON a.patient_id = p.id
                LEFT JOIN medical_history mh ON mh.patient_id = p.id
                WHERE p.user_id = ?
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
             PreparedStatement statement = connection.prepareStatement("DELETE FROM patients WHERE user_id=?")) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private void fill(PreparedStatement statement, Patient patient) throws java.sql.SQLException {
        if (patient.getUserId() == null) {
            statement.setNull(1, java.sql.Types.INTEGER);
        } else {
            statement.setInt(1, patient.getUserId());
        }
        statement.setString(2, patient.getFirstName());
        statement.setString(3, patient.getLastName());
        statement.setString(4, patient.getGender() == null ? null : patient.getGender().name());
        statement.setDate(5, patient.getDateOfBirth() == null ? null : Date.valueOf(patient.getDateOfBirth()));
        statement.setString(6, patient.getPhone());
        statement.setString(7, patient.getEmail());
        statement.setString(8, patient.getAddress());
        statement.setString(9, patient.getBloodGroup());
        statement.setString(10, patient.getAllergies());
        statement.setString(11, patient.getEmergencyContact());
    }

    private Patient map(ResultSet rs) throws java.sql.SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        patient.setUserId(rs.wasNull() ? null : userId);
        patient.setFirstName(rs.getString("first_name"));
        patient.setLastName(rs.getString("last_name"));
        String gender = rs.getString("gender");
        patient.setGender(gender == null || gender.isBlank() ? null : Gender.valueOf(gender.toUpperCase()));
        patient.setDateOfBirth(DateTimeUtil.toLocalDate(rs.getDate("date_of_birth")));
        patient.setPhone(rs.getString("phone"));
        patient.setEmail(rs.getString("email"));
        patient.setAddress(rs.getString("address"));
        patient.setBloodGroup(rs.getString("blood_group"));
        patient.setAllergies(rs.getString("allergies"));
        patient.setEmergencyContact(rs.getString("emergency_contact"));
        patient.setCreatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        return patient;
    }
}
