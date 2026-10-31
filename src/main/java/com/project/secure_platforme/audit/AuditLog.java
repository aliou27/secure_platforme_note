package com.project.secure_platforme.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;

@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(name = "username", length = 50, updatable = false)
    private String username;

    @Generated
    @Column(name = "timestamp", insertable = false, updatable = false)
    private Instant timestamp;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address", updatable = false)
    private InetAddress ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", updatable = false)
    private Map<String, Object> details;

    protected AuditLog() {
    }

    public AuditLog(String eventType, String username, InetAddress ipAddress, Map<String, Object> details) {
        this.eventType = eventType;
        this.username = username;
        this.ipAddress = ipAddress;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getUsername() {
        return username;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}