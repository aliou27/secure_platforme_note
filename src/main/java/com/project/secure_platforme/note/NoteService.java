package com.project.secure_platforme.note;

import com.project.secure_platforme.audit.AuditEventType;
import com.project.secure_platforme.audit.AuditService;
import com.project.secure_platforme.exception.NoteNotFoundException;
import com.project.secure_platforme.user.User;
import com.project.secure_platforme.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class NoteService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));

    private final NoteRepository noteRepository;
    private final UserService userService;
    private final AuditService auditService;

    public NoteService(NoteRepository noteRepository, UserService userService, AuditService auditService) {
        this.noteRepository = noteRepository;
        this.userService = userService;
        this.auditService = auditService;
    }

    @Transactional
    public Note create(Long userId, String title, String content) {
        User owner = userService.getById(userId);
        Note note = noteRepository.saveAndFlush(new Note(owner, title, content));
        auditService.record(AuditEventType.NOTE_CREATED, owner.getUsername(), Map.of("noteId", note.getId()));
        return note;
    }

    @Transactional(readOnly = true)
    public Page<Note> list(Long userId, int page, int size) {
        return noteRepository.findAllByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST));
    }

    @Transactional(readOnly = true)
    public Note get(Long userId, Long noteId) {
        return findOwned(userId, noteId, "read");
    }

    @Transactional
    public Note update(Long userId, Long noteId, String title, String content) {
        Note note = findOwned(userId, noteId, "update");
        note.update(title, content);
        Note saved = noteRepository.saveAndFlush(note);
        auditService.record(AuditEventType.NOTE_UPDATED, saved.getUser().getUsername(), Map.of("noteId", noteId));
        return saved;
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        Note note = findOwned(userId, noteId, "delete");
        String username = note.getUser().getUsername();
        noteRepository.delete(note);
        noteRepository.flush();
        auditService.record(AuditEventType.NOTE_DELETED, username, Map.of("noteId", noteId));
    }

    private Note findOwned(Long userId, Long noteId, String action) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> {
                    if (noteRepository.existsById(noteId)) {
                        String username = userService.getById(userId).getUsername();
                        auditService.record(AuditEventType.UNAUTHORIZED_ACCESS, username, Map.of(
                                "reason", "not_owner",
                                "action", action,
                                "noteId", noteId
                        ));
                    }
                    return new NoteNotFoundException();
                });
    }
}
