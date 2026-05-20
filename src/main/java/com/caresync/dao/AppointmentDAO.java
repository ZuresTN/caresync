package com.caresync.dao;

import com.caresync.models.Appointment;
import com.caresync.models.AppointmentStatus;
import com.caresync.utils.Database;
import com.caresync.utils.DateTimeUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {
    private static final String SELECT_JOIN = """
            SELECT a.*, CONCAT(p.first_name, ' ', p.last_name) AS patient_name,
                   p.email AS patient_email, p.phone AS patient_phone, u.full_name AS doctor_name
            FROM appointments a
            JOIN patients p ON p.id = a.patient_id
            JOIN doctors d ON d.id = a.doctor_id
            JOIN users u ON u.id = d.user_id
            """;

    public List<Appointment> findAll(String search, AppointmentStatus status, LocalDate date) throws java.sql.SQLException {
        String filter = search == null ? "" : search.trim();
        String sql = SELECT_JOIN + """
                WHERE (? = '' OR p.first_name LIKE ? OR p.last_name LIKE ? OR u.full_name LIKE ? OR a.reason LIKE ?)
                  AND (? IS NULL OR a.status = ?)
                  AND (? IS NULL OR a.appointment_date = ?)
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;
        List<Appointment> appointments = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + filter + "%";
            statement.setString(1, filter);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, like);
            statement.setString(6, status == null ? null : status.name());
            statement.setString(7, status == null ? null : status.name());
            statement.setDate(8, date == null ? null : Date.valueOf(date));
            statement.setDate(9, date == null ? null : Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(map(rs));
                }
            }
        }
        return appointments;
    }

    public List<Appointment> findAllBetween(String search, AppointmentStatus status, LocalDate start, LocalDate end) throws java.sql.SQLException {
        String filter = search == null ? "" : search.trim();
        String sql = SELECT_JOIN + """
                WHERE (? = '' OR p.first_name LIKE ? OR p.last_name LIKE ? OR u.full_name LIKE ? OR a.reason LIKE ?)
                  AND (? IS NULL OR a.status = ?)
                  AND a.appointment_date BETWEEN ? AND ?
                ORDER BY a.appointment_date ASC, a.appointment_time ASC
                """;
        List<Appointment> appointments = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + filter + "%";
            statement.setString(1, filter);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, like);
            statement.setString(6, status == null ? null : status.name());
            statement.setString(7, status == null ? null : status.name());
            statement.setDate(8, Date.valueOf(start));
            statement.setDate(9, Date.valueOf(end));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(map(rs));
                }
            }
        }
        return appointments;
    }

    public List<Appointment> findByDoctorUserId(int doctorUserId) throws java.sql.SQLException {
        String sql = SELECT_JOIN + """
                WHERE d.user_id = ?
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;
        List<Appointment> appointments = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(map(rs));
                }
            }
        }
        return appointments;
    }

    public List<Appointment> findByDoctorUserId(int doctorUserId, String search, AppointmentStatus status, LocalDate date) throws java.sql.SQLException {
        String filter = search == null ? "" : search.trim();
        String sql = SELECT_JOIN + """
                WHERE d.user_id = ?
                  AND (? = '' OR p.first_name LIKE ? OR p.last_name LIKE ? OR u.full_name LIKE ? OR a.reason LIKE ?)
                  AND (? IS NULL OR a.status = ?)
                  AND (? IS NULL OR a.appointment_date = ?)
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;
        List<Appointment> appointments = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + filter + "%";
            statement.setInt(1, doctorUserId);
            statement.setString(2, filter);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, like);
            statement.setString(6, like);
            statement.setString(7, status == null ? null : status.name());
            statement.setString(8, status == null ? null : status.name());
            statement.setDate(9, date == null ? null : Date.valueOf(date));
            statement.setDate(10, date == null ? null : Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(map(rs));
                }
            }
        }
        return appointments;
    }

    public List<Appointment> findByDoctorUserIdBetween(int doctorUserId, String search, AppointmentStatus status, LocalDate start, LocalDate end) throws java.sql.SQLException {
        String filter = search == null ? "" : search.trim();
        String sql = SELECT_JOIN + """
                WHERE d.user_id = ?
                  AND (? = '' OR p.first_name LIKE ? OR p.last_name LIKE ? OR u.full_name LIKE ? OR a.reason LIKE ?)
                  AND (? IS NULL OR a.status = ?)
                  AND a.appointment_date BETWEEN ? AND ?
                ORDER BY a.appointment_date ASC, a.appointment_time ASC
                """;
        List<Appointment> appointments = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + filter + "%";
            statement.setInt(1, doctorUserId);
            statement.setString(2, filter);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, like);
            statement.setString(6, like);
            statement.setString(7, status == null ? null : status.name());
            statement.setString(8, status == null ? null : status.name());
            statement.setDate(9, Date.valueOf(start));
            statement.setDate(10, Date.valueOf(end));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(map(rs));
                }
            }
        }
        return appointments;
    }

    public List<Appointment> findByPatientUserId(int patientUserId) throws java.sql.SQLException {
        String sql = SELECT_JOIN + """
                WHERE p.user_id = ?
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                """;
        List<Appointment> appointments = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(map(rs));
                }
            }
        }
        return appointments;
    }

    public int save(Appointment appointment) throws java.sql.SQLException {
        if (appointment.getId() > 0) {
            String sql = """
                    UPDATE appointments SET patient_id=?, doctor_id=?, appointment_date=?, appointment_time=?,
                    status=?, reason=?, notes=? WHERE id=?
                    """;
            try (Connection connection = Database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                fill(statement, appointment);
                statement.setInt(8, appointment.getId());
                statement.executeUpdate();
                return appointment.getId();
            }
        }

        String sql = """
                INSERT INTO appointments(patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes)
                VALUES(?,?,?,?,?,?,?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(statement, appointment);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    appointment.setId(keys.getInt(1));
                }
            }
            return appointment.getId();
        }
    }

    public boolean hasDoctorConflict(int appointmentId, int doctorId, LocalDate date, java.time.LocalTime time) throws java.sql.SQLException {
        String sql = """
                SELECT COUNT(*) FROM appointments
                WHERE doctor_id = ?
                  AND appointment_date = ?
                  AND appointment_time = ?
                  AND status IN ('SCHEDULED', 'PENDING')
                  AND id <> ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));
            statement.setInt(4, appointmentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void updateStatus(int appointmentId, AppointmentStatus status) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE appointments SET status=? WHERE id=?")) {
            statement.setString(1, status.name());
            statement.setInt(2, appointmentId);
            statement.executeUpdate();
        }
    }

    public int countToday() throws java.sql.SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE()";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countByStatus(AppointmentStatus status) throws java.sql.SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM appointments WHERE status=?")) {
            statement.setString(1, status.name());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void fill(PreparedStatement statement, Appointment appointment) throws java.sql.SQLException {
        statement.setInt(1, appointment.getPatientId());
        statement.setInt(2, appointment.getDoctorId());
        statement.setDate(3, Date.valueOf(appointment.getAppointmentDate()));
        statement.setTime(4, Time.valueOf(appointment.getAppointmentTime()));
        statement.setString(5, appointment.getStatus().name());
        statement.setString(6, appointment.getReason());
        statement.setString(7, appointment.getNotes());
    }

    private Appointment map(ResultSet rs) throws java.sql.SQLException {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getInt("id"));
        appointment.setPatientId(rs.getInt("patient_id"));
        appointment.setDoctorId(rs.getInt("doctor_id"));
        appointment.setAppointmentDate(DateTimeUtil.toLocalDate(rs.getDate("appointment_date")));
        appointment.setAppointmentTime(DateTimeUtil.toLocalTime(rs.getTime("appointment_time")));
        appointment.setStatus(AppointmentStatus.valueOf(rs.getString("status")));
        appointment.setReason(rs.getString("reason"));
        appointment.setNotes(rs.getString("notes"));
        appointment.setCreatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        appointment.setPatientName(rs.getString("patient_name"));
        appointment.setPatientEmail(rs.getString("patient_email"));
        appointment.setPatientPhone(rs.getString("patient_phone"));
        appointment.setDoctorName(rs.getString("doctor_name"));
        return appointment;
    }
}
