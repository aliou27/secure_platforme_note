package com.project.secure_platforme.note;

import com.project.secure_platforme.exception.NoteNotFoundException;
import com.project.secure_platforme.user.User;
import com.project.secure_platforme.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));

    private final NoteRepository noteRepository;
    private final UserService userService;

    public NoteService(NoteRepository noteRepository, UserService userService) {
        this.noteRepository = noteRepository;
        this.userService = userService;
    }

    @Transactional
    public Note create(Long userId, String title, String content) {
        User owner = userService.getById(userId);
        return noteRepository.saveAndFlush(new Note(owner, title, content));
    }

    @Transactional(readOnly = true)
    public Page<Note> list(Long userId, int page, int size) {
        return noteRepository.findAllByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST));
    }

    @Transactional(readOnly = true)
    public Note get(Long userId, Long noteId) {
        return findOwned(userId, noteId);
    }

    @Transactional
    public Note update(Long userId, Long noteId, String title, String content) {
        Note note = findOwned(userId, noteId);
        note.update(title, content);
        return noteRepository.saveAndFlush(note);
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        noteRepository.delete(findOwned(userId, noteId));
    }

    private Note findOwned(Long userId, Long noteId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(NoteNotFoundException::new);
    }
}
