package com.project.secure_platforme.note.dto;

import com.project.secure_platforme.note.Note;
import org.springframework.data.domain.Page;

import java.util.List;

public record NotePageResponse(List<NoteResponse> items, int page, int size, long totalElements, int totalPages) {

    public static NotePageResponse from(Page<Note> notes) {
        return new NotePageResponse(
                notes.getContent().stream().map(NoteResponse::from).toList(),
                notes.getNumber(),
                notes.getSize(),
                notes.getTotalElements(),
                notes.getTotalPages()
        );
    }
}
