package com.project.secure_platforme.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        @Size(max = 20000, message = "Content must be at most 20000 characters")
        String content
) {
    @Override
    public String toString() {
        return "NoteRequest{[redacted]}";
    }
}
