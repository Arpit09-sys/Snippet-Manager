package com.snippet.service;

import com.snippet.entity.Snippet;
import com.snippet.entity.User;
import com.snippet.repository.SnippetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SnippetService {

    @Autowired
    private SnippetRepository snippetRepository;

    public List<Snippet> getAllSnippetsForUser(User user) {
        return snippetRepository.findByUser(user);
    }

    public Optional<Snippet> getSnippetByIdForUser(Long id, User user) {
        return snippetRepository.findByIdAndUser(id, user);
    }

    public Snippet createSnippet(Snippet snippet, User user) {
        validateSnippet(snippet);
        // Explicitly set ID to null so clients cannot force an existing ID on creation
        snippet.setId(null);
        snippet.setUser(user);
        snippet.setCreatedAt(LocalDateTime.now());
        return snippetRepository.save(snippet);
    }

    public Optional<Snippet> updateSnippet(Long id, Snippet updatedData, User user) {
        Optional<Snippet> existing = snippetRepository.findByIdAndUser(id, user);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        validateSnippet(updatedData);

        Snippet snippet = existing.get();
        snippet.setTitle(updatedData.getTitle().trim());
        snippet.setLanguage(updatedData.getLanguage().trim());
        snippet.setCode(updatedData.getCode().trim());
        snippet.setDescription(updatedData.getDescription() != null ? updatedData.getDescription().trim() : null);

        return Optional.of(snippetRepository.save(snippet));
    }

    public boolean deleteSnippet(Long id, User user) {
        Optional<Snippet> existing = snippetRepository.findByIdAndUser(id, user);
        if (existing.isEmpty()) {
            return false;
        }
        snippetRepository.delete(existing.get());
        return true;
    }

    public List<Snippet> searchSnippetsForUser(String language, String title, User user) {
        String cleanLanguage = (language != null && !language.trim().isEmpty()) ? language.trim() : null;
        String cleanTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : null;

        if (cleanLanguage != null && cleanTitle != null) {
            return snippetRepository.findByUserAndLanguageIgnoreCaseAndTitleContainingIgnoreCase(user, cleanLanguage, cleanTitle);
        } else if (cleanLanguage != null) {
            return snippetRepository.findByUserAndLanguageIgnoreCase(user, cleanLanguage);
        } else if (cleanTitle != null) {
            return snippetRepository.findByUserAndTitleContainingIgnoreCase(user, cleanTitle);
        } else {
            return snippetRepository.findByUser(user);
        }
    }

    private void validateSnippet(Snippet snippet) {
        if (snippet.getTitle() == null || snippet.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Snippet title is required");
        }
        if (snippet.getLanguage() == null || snippet.getLanguage().trim().isEmpty()) {
            throw new IllegalArgumentException("Programming language is required");
        }
        if (snippet.getCode() == null || snippet.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Snippet code is required");
        }
    }
}
