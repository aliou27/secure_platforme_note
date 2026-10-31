package com.project.secure_platforme.audit;

public enum AuditEventType {
    USER_REGISTERED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_EXPIRED,
    TOKEN_INVALID,
    PASSWORD_CHANGED,
    NOTE_CREATED,
    NOTE_UPDATED,
    NOTE_DELETED,
    NOTES_EXPORTED,
    UNAUTHORIZED_ACCESS
}
