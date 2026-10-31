package com.project.secure_platforme.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate independentTransaction;

    public AuditService(AuditLogRepository auditLogRepository, PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.independentTransaction = new TransactionTemplate(transactionManager);
        this.independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(AuditEventType eventType, String username) {
        record(eventType, username, Map.of());
    }

    public void record(AuditEventType eventType, String username, Map<String, Object> details) {
        try {
            AuditLog entry = new AuditLog(
                    eventType.name(),
                    username,
                    clientAddress(),
                    details.isEmpty() ? null : details
            );
            independentTransaction.executeWithoutResult(status -> auditLogRepository.save(entry));
        } catch (RuntimeException e) {
            log.error("Failed to write audit event {}", eventType, e);
        }
    }

    private static InetAddress clientAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        String remoteAddress = attributes.getRequest().getRemoteAddr();
        if (remoteAddress == null) {
            return null;
        }
        try {
            return InetAddress.getByName(remoteAddress);
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
