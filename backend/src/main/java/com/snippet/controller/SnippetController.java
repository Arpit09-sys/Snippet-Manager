package com.snippet.controller;

import com.snippet.entity.Snippet;
import com.snippet.service.SnippetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/snippets")
@CrossOrigin(origins = "*")
public class SnippetController {

    @Autowired
    private SnippetService snippetService;

    // GET /api/snippets - get all snippets
    @GetMapping
    public List<Snippet> getAllSnippets() {
        return snippetService.getAllSnippets();
    }

    // GET /api/snippets/{id} - get one snippet
    @GetMapping("/{id}")
    public ResponseEntity<Snippet> getSnippetById(@PathVariable String id) {
        Optional<Snippet> snippet = snippetService.getSnippetById(id);
        if (snippet.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snippet.get());
    }

    // POST /api/snippets - create a snippet
    @PostMapping
    public Snippet createSnippet(@RequestBody Snippet snippet) {
        return snippetService.createSnippet(snippet);
    }

    // PUT /api/snippets/{id} - update a snippet
    @PutMapping("/{id}")
    public ResponseEntity<Snippet> updateSnippet(@PathVariable String id, @RequestBody Snippet snippet) {
        Optional<Snippet> updated = snippetService.updateSnippet(id, snippet);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated.get());
    }

    // DELETE /api/snippets/{id} - delete a snippet
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSnippet(@PathVariable String id) {
        boolean deleted = snippetService.deleteSnippet(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // GET /api/snippets/search?language=java&title=loop - search snippets
    @GetMapping("/search")
    public List<Snippet> searchSnippets(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String title) {
        return snippetService.searchSnippets(language, title);
    }
}
