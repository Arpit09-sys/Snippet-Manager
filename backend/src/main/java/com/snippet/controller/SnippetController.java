package com.snippet.controller;

import com.snippet.entity.Snippet;
import com.snippet.entity.User;
import com.snippet.service.SnippetService;
import com.snippet.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/snippets")
public class SnippetController {

    @Autowired
    private SnippetService snippetService;

    @Autowired
    private UserService userService;

    // Helper to get authenticated User entity from Spring Security session
    private User getAuthenticatedUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }

    // GET /api/snippets - get all snippets belonging to the logged-in user
    @GetMapping
    public List<Snippet> getAllSnippets(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return snippetService.getAllSnippetsForUser(user);
    }

    // GET /api/snippets/{id} - get one snippet (only if owned by logged-in user)
    @GetMapping("/{id}")
    public ResponseEntity<?> getSnippetById(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Optional<Snippet> snippet = snippetService.getSnippetByIdForUser(id, user);
        if (snippet.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Snippet not found or access denied"));
        }
        return ResponseEntity.ok(snippet.get());
    }

    // POST /api/snippets - create a snippet assigned to the logged-in user
    @PostMapping
    public ResponseEntity<?> createSnippet(@RequestBody Snippet snippet, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            Snippet created = snippetService.createSnippet(snippet, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // PUT /api/snippets/{id} - update a snippet (only if owned by logged-in user)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSnippet(@PathVariable Long id, @RequestBody Snippet snippet, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            Optional<Snippet> updated = snippetService.updateSnippet(id, snippet, user);
            if (updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Snippet not found or access denied"));
            }
            return ResponseEntity.ok(updated.get());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // DELETE /api/snippets/{id} - delete a snippet (only if owned by logged-in user)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSnippet(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        boolean deleted = snippetService.deleteSnippet(id, user);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Snippet not found or access denied"));
        }
        return ResponseEntity.noContent().build();
    }

    // GET /api/snippets/search - search snippets belonging to the logged-in user
    @GetMapping("/search")
    public List<Snippet> searchSnippets(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String title,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return snippetService.searchSnippetsForUser(language, title, user);
    }
}
