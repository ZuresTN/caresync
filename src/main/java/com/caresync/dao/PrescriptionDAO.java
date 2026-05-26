package com.caresync.dao;

import com.caresync.models.Prescription;
import com.caresync.models.PrescriptionItem;
import com.caresync.utils.Database;
import com.caresync.utils.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrescriptionDAO {
    public int saveWithItems(Prescription prescription) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int prescriptionId = insertPrescription(connection, prescription);
                for (PrescriptionItem item : prescription.getItems()) {
                    insertItem(connection, prescriptionId, item);
                }
                connection.commit();
                prescription.setId(prescriptionId);
                return prescriptionId;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Optional<Prescription> findByMedicalHistoryId(int medicalHistoryId) throws java.sql.SQLException {
        String sql = "SELECT * FROM prescriptions WHERE medical_history_id=? ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medicalHistoryId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Prescription prescription = new Prescription();
                    prescription.setId(rs.getInt("id"));
                    prescription.setMedicalHistoryId(rs.getInt("medical_history_id"));
                    prescription.setInstructions(rs.getString("instructions"));
                    prescription.setCreatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("created_at")));
                    prescription.setItems(findItems(prescription.getId()));
                    return Optional.of(prescription);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Prescription> findByMedicalRecordId(int medicalRecordId) throws java.sql.SQLException {
        return findByMedicalHistoryId(medicalRecordId);
    }

    private int insertPrescription(Connection connection, Prescription prescription) throws java.sql.SQLException {
        String sql = "INSERT INTO prescriptions(medical_history_id, instructions) VALUES(?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, prescription.getMedicalHistoryId());
            statement.setString(2, prescription.getInstructions());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new java.sql.SQLException("Prescription insert failed.");
    }

    private void insertItem(Connection connection, int prescriptionId, PrescriptionItem item) throws java.sql.SQLException {
        String sql = """
                INSERT INTO prescription_items(prescription_id, medicine_name, dosage, frequency, duration, notes)
                VALUES(?,?,?,?,?,?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, prescriptionId);
            statement.setString(2, item.getMedicineName());
            statement.setString(3, item.getDosage());
            statement.setString(4, item.getFrequency());
            statement.setString(5, item.getDuration());
            statement.setString(6, item.getNotes());
            statement.executeUpdate();
        }
    }

    private List<PrescriptionItem> findItems(int prescriptionId) throws java.sql.SQLException {
        String sql = "SELECT * FROM prescription_items WHERE prescription_id=?";
        List<PrescriptionItem> items = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, prescriptionId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PrescriptionItem item = new PrescriptionItem();
                    item.setId(rs.getInt("id"));
                    item.setPrescriptionId(rs.getInt("prescription_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setDosage(rs.getString("dosage"));
                    item.setFrequency(rs.getString("frequency"));
                    item.setDuration(rs.getString("duration"));
                    item.setNotes(rs.getString("notes"));
                    items.add(item);
                }
            }
        }
        return items;
    }
}
