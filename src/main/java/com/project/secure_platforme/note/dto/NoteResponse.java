package com.project.secure_platforme.note.dto;

import com.project.secure_platforme.note.Note;

import java.time.Instant;

public record NoteResponse(Long id, String title, String content, Instant createdAt, Instant updatedAt) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getCreatedAt(), note.getUpdatedAt());
    }

    @Override
    public String toString() {
        return "NoteResponse{id=" + id + "}";
    }
}
