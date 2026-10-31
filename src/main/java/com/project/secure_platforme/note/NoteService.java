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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class NoteService {

    public static final int MAX_EXPORT_NOTES = 1000;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final NoteRepository noteRepository;
    private final UserService userService;
    private final AuditService auditService;
    private final NotePdfExporter notePdfExporter;

    public NoteService(NoteRepository noteRepository,
                       UserService userService,
                       AuditService auditService,
                       NotePdfExporter notePdfExporter) {
        this.noteRepository = noteRepository;
        this.userService = userService;
        this.auditService = auditService;
        this.notePdfExporter = notePdfExporter;
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

    @Transactional(readOnly = true)
    public PdfExport exportPdf(Long userId) {
        User owner = userService.getById(userId);
        Page<Note> notes = noteRepository.findAllByUserId(userId, PageRequest.of(0, MAX_EXPORT_NOTES, NEWEST_FIRST));
        Instant exportedAt = Instant.now();

        byte[] content = notePdfExporter.export(owner.getUsername(), notes.getContent(), notes.getTotalElements(), exportedAt);

        auditService.record(AuditEventType.NOTES_EXPORTED, owner.getUsername(), Map.of(
                "exportedNotes", notes.getNumberOfElements(),
                "totalNotes", notes.getTotalElements()
        ));

        String filename = "notes-" + owner.getUsername() + "-" + FILE_DATE.format(exportedAt) + ".pdf";
        return new PdfExport(filename, content);
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

    public record PdfExport(String filename, byte[] content) {
        @Override
        public String toString() {
            return "PdfExport{filename='" + filename + "', bytes=" + content.length + "}";
        }
    }
}
