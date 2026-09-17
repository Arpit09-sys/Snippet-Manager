package com.snippet.service;

import com.snippet.entity.Snippet;
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

    // Get all snippets
    public List<Snippet> getAllSnippets() {
        return snippetRepository.findAll();
    }

    // Get one snippet by id
    public Optional<Snippet> getSnippetById(String id) {
        return snippetRepository.findById(id);
    }

    // Create a new snippet
    public Snippet createSnippet(Snippet snippet) {
        snippet.setCreatedAt(LocalDateTime.now());
        return snippetRepository.save(snippet);
    }

    // Update an existing snippet
    public Optional<Snippet> updateSnippet(String id, Snippet updatedData) {
        Optional<Snippet> existing = snippetRepository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        Snippet snippet = existing.get();
        snippet.setTitle(updatedData.getTitle());
        snippet.setCode(updatedData.getCode());
        snippet.setLanguage(updatedData.getLanguage());
        snippet.setDescription(updatedData.getDescription());

        return Optional.of(snippetRepository.save(snippet));
    }

    // Delete a snippet
    public boolean deleteSnippet(String id) {
        if (!snippetRepository.existsById(id)) {
            return false;
        }
        snippetRepository.deleteById(id);
        return true;
    }

    // Search by language and/or title
    public List<Snippet> searchSnippets(String language, String title) {
        if (language != null && title != null) {
            return snippetRepository.findByLanguageIgnoreCaseAndTitleContainingIgnoreCase(language, title);
        } else if (language != null) {
            return snippetRepository.findByLanguageIgnoreCase(language);
        } else if (title != null) {
            return snippetRepository.findByTitleContainingIgnoreCase(title);
        } else {
            return snippetRepository.findAll();
        }
    }
}
