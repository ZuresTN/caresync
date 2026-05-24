package com.caresync.dao;

import com.caresync.models.MedicalHistory;
import com.caresync.utils.Database;
import com.caresync.utils.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MedicalHistoryDAO {
    private static final String SELECT_JOIN = """
            SELECT mh.*, CONCAT(p.first_name, ' ', p.last_name) AS patient_name, u.full_name AS doctor_name
            FROM medical_history mh
            JOIN patients p ON p.id = mh.patient_id
            JOIN doctors d ON d.id = mh.doctor_id
            JOIN users u ON u.id = d.user_id
            """;

    public List<MedicalHistory> findByPatientId(int patientId) throws java.sql.SQLException {
        String sql = SELECT_JOIN + " WHERE mh.patient_id = ? ORDER BY mh.created_at DESC";
        List<MedicalHistory> records = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        }
        return records;
    }

    public List<MedicalHistory> findByPatientIdAndDoctorUserId(int patientId, int doctorUserId) throws java.sql.SQLException {
        String sql = SELECT_JOIN + " WHERE mh.patient_id = ? AND d.user_id = ? ORDER BY mh.created_at DESC";
        List<MedicalHistory> records = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, doctorUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        }
        return records;
    }

    public List<MedicalHistory> findByDoctorUserId(int doctorUserId) throws java.sql.SQLException {
        String sql = SELECT_JOIN + " WHERE d.user_id = ? ORDER BY mh.created_at DESC";
        List<MedicalHistory> records = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        }
        return records;
    }

    public List<MedicalHistory> findByPatientUserId(int patientUserId) throws java.sql.SQLException {
        String sql = SELECT_JOIN + " WHERE p.user_id = ? ORDER BY mh.created_at DESC";
        List<MedicalHistory> records = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        }
        return records;
    }

    public int save(MedicalHistory record) throws java.sql.SQLException {
        String sql = """
                INSERT INTO medical_history(patient_id, doctor_id, appointment_id, diagnosis, treatment_notes)
                VALUES(?,?,?,?,?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, record.getPatientId());
            statement.setInt(2, record.getDoctorId());
            if (record.getAppointmentId() == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, record.getAppointmentId());
            }
            statement.setString(4, record.getDiagnosis());
            statement.setString(5, record.getTreatmentNotes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    record.setId(keys.getInt(1));
                }
            }
            return record.getId();
        }
    }

    public int countAll() throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM medical_history");
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean existsForAppointment(int appointmentId) throws java.sql.SQLException {
        String sql = "SELECT COUNT(*) FROM medical_history WHERE appointment_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, appointmentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private MedicalHistory map(ResultSet rs) throws java.sql.SQLException {
        MedicalHistory record = new MedicalHistory();
        record.setId(rs.getInt("id"));
        record.setPatientId(rs.getInt("patient_id"));
        record.setDoctorId(rs.getInt("doctor_id"));
        int appointmentId = rs.getInt("appointment_id");
        record.setAppointmentId(rs.wasNull() ? null : appointmentId);
        record.setDiagnosis(rs.getString("diagnosis"));
        record.setTreatmentNotes(rs.getString("treatment_notes"));
        record.setCreatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        record.setPatientName(rs.getString("patient_name"));
        record.setDoctorName(rs.getString("doctor_name"));
        return record;
    }
}
