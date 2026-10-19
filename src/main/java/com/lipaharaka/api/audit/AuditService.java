package com.lipaharaka.api.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorUserId, String action, String entityType, UUID entityId, Map<String, Object> metadata) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(metadata == null ? Map.of() : metadata);
        repository.save(log);
    }
}
