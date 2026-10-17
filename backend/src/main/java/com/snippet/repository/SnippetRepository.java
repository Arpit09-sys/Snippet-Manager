package com.snippet.repository;

import com.snippet.entity.Snippet;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    // Get all snippets belonging to a specific user
    List<Snippet> findByUser(User user);

    // Get a specific snippet by id only if it belongs to the specified user
    Optional<Snippet> findByIdAndUser(Long id, User user);

    // Check if snippet exists and belongs to the specified user
    boolean existsByIdAndUser(Long id, User user);

    // Search user's snippets by language (case-insensitive)
    List<Snippet> findByUserAndLanguageIgnoreCase(User user, String language);

    // Search user's snippets by title keyword (case-insensitive)
    List<Snippet> findByUserAndTitleContainingIgnoreCase(User user, String title);

    // Search user's snippets by both language and title keyword
    List<Snippet> findByUserAndLanguageIgnoreCaseAndTitleContainingIgnoreCase(User user, String language, String title);
}
