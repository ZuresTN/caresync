package com.caresync.dao;

import com.caresync.models.ReminderQueueItem;
import com.caresync.models.ReminderStatus;
import com.caresync.utils.Database;
import com.caresync.utils.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderQueueDAO {
    public int save(ReminderQueueItem item) throws java.sql.SQLException {
        String sql = """
                INSERT INTO reminder_queue(appointment_id, recipient_email, recipient_phone, channel, status, message, scheduled_at)
                VALUES(?,?,?,?,?,?,?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, item.getAppointmentId());
            statement.setString(2, item.getRecipientEmail());
            statement.setString(3, item.getRecipientPhone());
            statement.setString(4, item.getChannel());
            statement.setString(5, item.getStatus().name());
            statement.setString(6, item.getMessage());
            statement.setTimestamp(7, Timestamp.valueOf(item.getScheduledAt() == null ? LocalDateTime.now() : item.getScheduledAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
            return item.getId();
        }
    }

    public List<ReminderQueueItem> findDuePending(LocalDateTime now) throws java.sql.SQLException {
        String sql = """
                SELECT * FROM reminder_queue
                WHERE status='PENDING' AND scheduled_at <= ?
                ORDER BY scheduled_at ASC
                LIMIT 50
                """;
        List<ReminderQueueItem> items = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(now));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    public void markSent(int id) throws java.sql.SQLException {
        updateStatus(id, ReminderStatus.SENT, null, true);
    }

    public void markFailed(int id, String errorMessage) throws java.sql.SQLException {
        updateStatus(id, ReminderStatus.FAILED, errorMessage, false);
    }

    private void updateStatus(int id, ReminderStatus status, String errorMessage, boolean sent) throws java.sql.SQLException {
        String sql = "UPDATE reminder_queue SET status=?, error_message=?, sent_at=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, errorMessage);
            statement.setTimestamp(3, sent ? Timestamp.valueOf(LocalDateTime.now()) : null);
            statement.setInt(4, id);
            statement.executeUpdate();
        }
    }

    private ReminderQueueItem map(ResultSet rs) throws java.sql.SQLException {
        ReminderQueueItem item = new ReminderQueueItem();
        item.setId(rs.getInt("id"));
        item.setAppointmentId(rs.getInt("appointment_id"));
        item.setRecipientEmail(rs.getString("recipient_email"));
        item.setRecipientPhone(rs.getString("recipient_phone"));
        item.setChannel(rs.getString("channel"));
        item.setStatus(ReminderStatus.valueOf(rs.getString("status")));
        item.setMessage(rs.getString("message"));
        item.setErrorMessage(rs.getString("error_message"));
        item.setScheduledAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("scheduled_at")));
        item.setSentAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("sent_at")));
        item.setCreatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        return item;
    }
}
