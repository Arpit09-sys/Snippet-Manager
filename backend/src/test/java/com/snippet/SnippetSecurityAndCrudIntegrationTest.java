package com.snippet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snippet.entity.Snippet;
import com.snippet.entity.User;
import com.snippet.repository.SnippetRepository;
import com.snippet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SnippetSecurityAndCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SnippetRepository snippetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        snippetRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testDatabaseConnectionAndContextLoads() {
        assertNotNull(userRepository);
        assertNotNull(snippetRepository);
        assertEquals(0, userRepository.count());
        assertEquals(0, snippetRepository.count());
    }

    @Test
    void testUserRegistrationAndBCryptHashing() throws Exception {
        Map<String, String> regRequest = Map.of(
                "username", "john_dev",
                "password", "secretPassword123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.username").value("john_dev"));

        Optional<User> savedUserOpt = userRepository.findByUsername("john_dev");
        assertTrue(savedUserOpt.isPresent());

        User savedUser = savedUserOpt.get();
        // Password must NEVER be stored as plain-text
        assertNotEquals("secretPassword123", savedUser.getPassword());
        // Must be a valid BCrypt hash format (starts with $2a$ or $2b$)
        assertTrue(savedUser.getPassword().startsWith("$2a$") || savedUser.getPassword().startsWith("$2b$"));
        // Standard password verification matches correctly
        assertTrue(passwordEncoder.matches("secretPassword123", savedUser.getPassword()));
    }

    @Test
    void testRegistrationValidationAndDuplicateUsername() throws Exception {
        // Blank username rejected
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "", "password", "pass123"))))
                .andExpect(status().isBadRequest());

        // Short password rejected
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "validUser", "password", "123"))))
                .andExpect(status().isBadRequest());

        // Register valid user
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "alice", "password", "alice123"))))
                .andExpect(status().isCreated());

        // Duplicate username rejected
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "alice", "password", "differentPass"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username is already taken"));
    }

    @Test
    void testLoginWithValidAndInvalidCredentials() throws Exception {
        // First register user
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "bob", "password", "password123"))))
                .andExpect(status().isCreated());

        // Login with wrong password should fail with 401
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "bob", "password", "wrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));

        // Login with non-existent user should fail with 401
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "ghost", "password", "password123"))))
                .andExpect(status().isUnauthorized());

        // Login with valid credentials succeeds and returns session
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "bob", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.username").value("bob"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertNotNull(session);

        // Status endpoint confirms logged in user
        mockMvc.perform(get("/api/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void testUnauthenticatedUserCannotAccessSnippets() throws Exception {
        // Without session, GET snippets is rejected with 401
        mockMvc.perform(get("/api/snippets"))
                .andExpect(status().isUnauthorized());

        // Without session, POST snippet with valid CSRF is still rejected with 401 Unauthorized
        mockMvc.perform(post("/api/snippets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"language\":\"Java\",\"code\":\"System.out.println();\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCsrfProtectionRejectsRequestWithoutCsrf() throws Exception {
        // Register and login
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "csrfUser", "password", "password123"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "csrfUser", "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // Making a POST request without CSRF token is blocked with 403 Forbidden
        mockMvc.perform(post("/api/snippets")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"CSRF Attack\",\"language\":\"Java\",\"code\":\"badCode()\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSnippetCrudForAuthenticatedUser() throws Exception {
        // 1. Register and login
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "coder", "password", "coderPass1"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "coder", "password", "coderPass1"))))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // 2. Initially empty
        mockMvc.perform(get("/api/snippets").session(session))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // 3. Create snippet
        Map<String, String> snippetPayload = Map.of(
                "title", "Binary Search",
                "language", "Java",
                "description", "Iterative binary search algorithm",
                "code", "int binarySearch(int[] arr, int x) { return 0; }"
        );

        MvcResult createResult = mockMvc.perform(post("/api/snippets")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(snippetPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Binary Search"))
                .andExpect(jsonPath("$.language").value("Java"))
                .andReturn();

        Snippet createdSnippet = objectMapper.readValue(createResult.getResponse().getContentAsString(), Snippet.class);
        Long snippetId = createdSnippet.getId();
        assertNotNull(snippetId);

        // 4. View snippet by ID
        mockMvc.perform(get("/api/snippets/" + snippetId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Binary Search"));

        // 5. Update snippet
        Map<String, String> updatePayload = Map.of(
                "title", "Binary Search (Optimized)",
                "language", "Java",
                "description", "Optimized with mid calculation",
                "code", "int mid = low + (high - low) / 2;"
        );

        mockMvc.perform(put("/api/snippets/" + snippetId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Binary Search (Optimized)"))
                .andExpect(jsonPath("$.code").value("int mid = low + (high - low) / 2;"));

        // 6. Delete snippet
        mockMvc.perform(delete("/api/snippets/" + snippetId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // 7. Verify deletion
        mockMvc.perform(get("/api/snippets/" + snippetId).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUserIsolationAndOwnershipAccessControl() throws Exception {
        // Register Alice and Bob
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "alice", "password", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "bob", "password", "password123"))))
                .andExpect(status().isCreated());

        // Alice logs in and creates a snippet
        MvcResult aliceLogin = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "alice", "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession aliceSession = (MockHttpSession) aliceLogin.getRequest().getSession();

        MvcResult snippetResult = mockMvc.perform(post("/api/snippets")
                        .session(aliceSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Alice Private Key Generator",
                                "language", "Python",
                                "code", "def generate_key(): return 'secret'"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        Snippet aliceSnippet = objectMapper.readValue(snippetResult.getResponse().getContentAsString(), Snippet.class);
        Long aliceSnippetId = aliceSnippet.getId();

        // Bob logs in
        MvcResult bobLogin = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "bob", "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession bobSession = (MockHttpSession) bobLogin.getRequest().getSession();

        // Bob views his snippets -> Should be empty! (Bob CANNOT see Alice's snippet)
        mockMvc.perform(get("/api/snippets").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Bob attempts to directly GET Alice's snippet by ID -> Denied with 404
        mockMvc.perform(get("/api/snippets/" + aliceSnippetId).session(bobSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Snippet not found or access denied"));

        // Bob attempts to maliciously PUT / overwrite Alice's snippet -> Denied with 404
        mockMvc.perform(put("/api/snippets/" + aliceSnippetId)
                        .session(bobSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Hacked Title",
                                "language", "Python",
                                "code", "tampered code"
                        ))))
                .andExpect(status().isNotFound());

        // Verify Alice's snippet was NOT modified in the database
        Snippet unchangedSnippet = snippetRepository.findById(aliceSnippetId).orElseThrow();
        assertEquals("Alice Private Key Generator", unchangedSnippet.getTitle());

        // Bob attempts to DELETE Alice's snippet -> Denied with 404
        mockMvc.perform(delete("/api/snippets/" + aliceSnippetId)
                        .session(bobSession)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        // Verify snippet still exists in DB
        assertTrue(snippetRepository.existsById(aliceSnippetId));
    }

    @Test
    void testSnippetSearchIsScopedToOwner() throws Exception {
        // Register Dev1 and Dev2
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "dev1", "password", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "dev2", "password", "password123"))))
                .andExpect(status().isCreated());

        // Dev1 creates Java snippet
        MvcResult dev1Login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "dev1", "password", "password123"))))
                .andReturn();
        MockHttpSession dev1Session = (MockHttpSession) dev1Login.getRequest().getSession();

        mockMvc.perform(post("/api/snippets").session(dev1Session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Dev1 Java QuickSort", "language", "Java", "code", "// Java code"))))
                .andExpect(status().isCreated());

        // Dev2 creates Java snippet
        MvcResult dev2Login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "dev2", "password", "password123"))))
                .andReturn();
        MockHttpSession dev2Session = (MockHttpSession) dev2Login.getRequest().getSession();

        mockMvc.perform(post("/api/snippets").session(dev2Session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Dev2 Java MergeSort", "language", "Java", "code", "// Java code"))))
                .andExpect(status().isCreated());

        // Dev1 searches for language "Java" -> only sees "Dev1 Java QuickSort", never Dev2's snippet
        mockMvc.perform(get("/api/snippets/search?language=Java").session(dev1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Dev1 Java QuickSort"));

        // Dev2 searches for title "QuickSort" -> sees empty list because it belongs to Dev1
        mockMvc.perform(get("/api/snippets/search?title=QuickSort").session(dev2Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testSnippetValidationErrors() throws Exception {
        // Register and login
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "valUser", "password", "password123"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "valUser", "password", "password123"))))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // Missing title
        mockMvc.perform(post("/api/snippets").session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "", "language", "Java", "code", "code"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Snippet title is required"));

        // Missing language
        mockMvc.perform(post("/api/snippets").session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Title", "language", "", "code", "code"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Programming language is required"));

        // Missing code
        mockMvc.perform(post("/api/snippets").session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Title", "language", "Java", "code", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Snippet code is required"));
    }
}
