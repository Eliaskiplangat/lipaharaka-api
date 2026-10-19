package com.lipaharaka.api.audit;

import com.lipaharaka.api.common.BaseEntity;
import com.lipaharaka.api.security.CurrentUserProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;

    public AuditAspect(AuditService auditService, CurrentUserProvider currentUserProvider) {
        this.auditService = auditService;
        this.currentUserProvider = currentUserProvider;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed(); // let business logic run and its own transaction commit first

        try {
            UUID actorId = resolveActor();
            UUID entityId = resolveEntityId(result);
            auditService.record(actorId, audited.action(), audited.entityType(), entityId, Map.of());
        } catch (Exception ex) {

            log.error("Failed to write audit log for action={} entityType={}", audited.action(), audited.entityType(), ex);
        }
        return result;
    }

    private UUID resolveActor() {
        try {
            return currentUserProvider.get().userId();
        } catch (Exception ex) {
            return null; // system-initiated actions (scheduler, webhook) have no human actor
        }
    }

    private UUID resolveEntityId(Object result) {
        if (result instanceof BaseEntity entity) {
            return entity.getId();
        }
        return null;
    }
}
