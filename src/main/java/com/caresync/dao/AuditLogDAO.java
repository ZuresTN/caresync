package com.caresync.dao;

import com.caresync.models.AuditLog;
import com.caresync.utils.Database;
import com.caresync.utils.DateTimeUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {
    public int save(AuditLog log) throws java.sql.SQLException {
        String sql = """
                INSERT INTO audit_logs(actor_user_id, action, entity_type, entity_id, summary)
                VALUES(?,?,?,?,?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getActorUserId() == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, log.getActorUserId());
            }
            statement.setString(2, log.getAction());
            statement.setString(3, log.getEntityType());
            if (log.getEntityId() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, log.getEntityId());
            }
            statement.setString(5, log.getSummary());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setId(keys.getInt(1));
                }
            }
            return log.getId();
        }
    }

    public List<AuditLog> findAll(String action, String entityType, String actor, LocalDate date) throws java.sql.SQLException {
        String sql = """
                SELECT al.*, u.full_name AS actor_name
                FROM audit_logs al
                LEFT JOIN users u ON u.id = al.actor_user_id
                WHERE (? = '' OR al.action LIKE ?)
                  AND (? = '' OR al.entity_type LIKE ?)
                  AND (? = '' OR u.full_name LIKE ? OR u.username LIKE ?)
                  AND (? IS NULL OR DATE(al.created_at) = ?)
                ORDER BY al.created_at DESC
                LIMIT 500
                """;
        List<AuditLog> logs = new ArrayList<>();
        String actionFilter = action == null ? "" : action.trim();
        String entityFilter = entityType == null ? "" : entityType.trim();
        String actorFilter = actor == null ? "" : actor.trim();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, actionFilter);
            statement.setString(2, "%" + actionFilter + "%");
            statement.setString(3, entityFilter);
            statement.setString(4, "%" + entityFilter + "%");
            statement.setString(5, actorFilter);
            statement.setString(6, "%" + actorFilter + "%");
            statement.setString(7, "%" + actorFilter + "%");
            statement.setDate(8, date == null ? null : Date.valueOf(date));
            statement.setDate(9, date == null ? null : Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    logs.add(map(rs));
                }
            }
        }
        return logs;
    }

    private AuditLog map(ResultSet rs) throws java.sql.SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getInt("id"));
        int actorId = rs.getInt("actor_user_id");
        log.setActorUserId(rs.wasNull() ? null : actorId);
        log.setActorName(rs.getString("actor_name"));
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        int entityId = rs.getInt("entity_id");
        log.setEntityId(rs.wasNull() ? null : entityId);
        log.setSummary(rs.getString("summary"));
        log.setCreatedAt(DateTimeUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        return log;
    }
}
