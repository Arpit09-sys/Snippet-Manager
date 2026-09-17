package com.snippet.repository;

import com.snippet.entity.Snippet;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SnippetRepository extends MongoRepository<Snippet, String> {

    // Find by language (case-insensitive)
    List<Snippet> findByLanguageIgnoreCase(String language);

    // Find by title containing a keyword (case-insensitive)
    List<Snippet> findByTitleContainingIgnoreCase(String title);

    // Find by both language and title
    List<Snippet> findByLanguageIgnoreCaseAndTitleContainingIgnoreCase(String language, String title);
}
