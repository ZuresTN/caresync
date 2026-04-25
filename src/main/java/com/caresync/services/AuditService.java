package com.caresync.services;

import com.caresync.dao.AuditLogDAO;
import com.caresync.models.AuditLog;
import com.caresync.models.User;
import com.caresync.utils.SessionManager;

public class AuditService {
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public void record(String action, String entityType, Integer entityId, String summary) throws java.sql.SQLException {
        AuditLog log = new AuditLog();
        User actor = SessionManager.getCurrentUser();
        log.setActorUserId(actor == null ? null : actor.getId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setSummary(summary);
        auditLogDAO.save(log);
    }
}
