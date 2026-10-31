package com.project.secure_platforme.note;

import com.project.secure_platforme.note.dto.NotePageResponse;
import com.project.secure_platforme.note.dto.NoteRequest;
import com.project.secure_platforme.note.dto.NoteResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody NoteRequest request) {
        Note note = noteService.create(currentUserId(jwt), request.title(), request.content());
        return ResponseEntity.created(URI.create("/api/notes/" + note.getId())).body(NoteResponse.from(note));
    }

    @GetMapping
    public NotePageResponse list(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return NotePageResponse.from(noteService.list(currentUserId(jwt), page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Jwt jwt) {
        NoteService.PdfExport export = noteService.exportPdf(currentUserId(jwt));
        ContentDisposition disposition = ContentDisposition.attachment().filename(export.filename()).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(export.content().length)
                .body(export.content());
    }

    @GetMapping("/{id}")
    public NoteResponse get(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable @Positive Long id) {
        return NoteResponse.from(noteService.get(currentUserId(jwt), id));
    }

    @PutMapping("/{id}")
    public NoteResponse update(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable @Positive Long id,
                               @Valid @RequestBody NoteRequest request) {
        return NoteResponse.from(noteService.update(currentUserId(jwt), id, request.title(), request.content()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable @Positive Long id) {
        noteService.delete(currentUserId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
