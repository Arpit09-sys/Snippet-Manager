# Developer Snippet Manager (Secure Edition)

A secure web application to save, organize, and search code snippets. Built with Spring Boot, Spring Data JPA, MySQL, Spring Security (BCrypt & session-based authentication), and Vanilla HTML/CSS/JavaScript.

## Project Structure

```
snippet-manager/
├── backend/
│   ├── pom.xml                                   ← Maven configuration with JPA, MySQL, Spring Security
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/snippet/
│   │   │   │   ├── config/
│   │   │   │   │   └── SecurityConfig.java       ← Spring Security filter chain, BCrypt, CSRF, sessions
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java       ← Register, login, status, CSRF endpoints
│   │   │   │   │   └── SnippetController.java    ← User-scoped snippet CRUD & search
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Snippet.java              ← JPA entity mapped to MySQL 'snippets' table
│   │   │   │   │   └── User.java                 ← JPA entity mapped to MySQL 'users' table
│   │   │   │   ├── repository/
│   │   │   │   │   ├── SnippetRepository.java    ← Spring Data JPA with user-ownership queries
│   │   │   │   │   └── UserRepository.java       ← JPA repository for user lookup & existence checks
│   │   │   │   ├── service/
│   │   │   │   │   ├── CustomUserDetailsService.java ← Loads user for Spring Security authentication
│   │   │   │   │   ├── SnippetService.java       ← Business logic & ownership enforcement
│   │   │   │   │   └── UserService.java          ← Registration with BCrypt hashing
│   │   │   │   └── SnippetManagerApplication.java← Spring Boot main entry point
│   │   │   └── resources/
│   │   │       ├── application.properties        ← MySQL connection & Hibernate settings
│   │   │       └── static/                       ← Frontend files served directly by Spring Boot
│   │   └── test/
│   │       └── java/com/snippet/
│   │           └── SnippetSecurityAndCrudIntegrationTest.java ← Automated test suite (10 test cases)
├── frontend/
│   ├── index.html                                ← UI with Login/Register tabs & Snippet Manager
│   ├── style.css                                 ← Clean, responsive styling
│   └── script.js                                 ← Session auth, CSRF handling & Fetch API calls
└── README.md
```

---

## Prerequisites

- **Java 17+** (Java 21 LTS supported)
- **Maven**
- **MySQL Server** running on port 3306

---

## Database Setup

1. Start your local MySQL service:
   ```bash
   brew services start mysql    # macOS (Homebrew)
   # OR: sudo systemctl start mysql  # Linux
   ```

2. Create the `snippetdb` database:
   ```bash
   mysql -u root -e "CREATE DATABASE IF NOT EXISTS snippetdb;"
   ```

3. Spring Boot with Hibernate automatically generates the required `users` and `snippets` tables and foreign keys upon application startup (`spring.jpa.hibernate.ddl-auto=update`).

---

## Running the Application

### 1. Run the Backend
```bash
cd backend
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

### 2. Access the Frontend
Open `http://localhost:8080` directly in your browser. (Alternatively, you can open `frontend/index.html` directly in your browser; the API calls will communicate with `http://localhost:8080`).

---

## Running Automated Tests

To run the complete automated integration test suite:
```bash
cd backend
mvn test
```

---

## API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Description | Public / Protected |
|--------|----------|-------------|--------------------|
| POST | `/api/auth/register` | Register a new user | Public |
| POST | `/api/auth/login` | Login and establish session | Public |
| POST | `/api/auth/logout` | Invalidate session & clear cookies | Authenticated |
| GET | `/api/auth/status` | Check current authenticated user | Public |
| GET | `/api/auth/csrf` | Get CSRF token for state-changing requests | Public |

### Snippets (`/api/snippets`)
*All snippet endpoints require authentication. Operations are strictly isolated to the authenticated user.*

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/snippets` | Get all snippets for the logged-in user |
| GET | `/api/snippets/{id}` | Get snippet by ID (returns 404 if owned by someone else) |
| POST | `/api/snippets` | Create a new snippet owned by the logged-in user |
| PUT | `/api/snippets/{id}` | Update snippet (returns 404 if owned by someone else) |
| DELETE | `/api/snippets/{id}` | Delete snippet (returns 404 if owned by someone else) |
| GET | `/api/snippets/search?language=java` | Search logged-in user's snippets by language |
| GET | `/api/snippets/search?title=sort` | Search logged-in user's snippets by title |
| GET | `/api/snippets/search?language=java&title=sort` | Search by both language and title |

---

## Key Cybersecurity Features

1. **BCrypt Password Hashing**: Passwords are never stored in plain text. Stored passwords use BCrypt with salting and work factor 10.
2. **Session-Based Authentication**: Uses standard Spring Security HTTP sessions with `JSESSIONID` cookie (`HttpOnly`).
3. **User-Scoped Authorization & Ownership**: The backend resolves the authenticated user from the session and enforces that users can only read, edit, or delete snippets belonging to them. Tampered IDs return `404 Not Found`.
4. **CSRF Protection**: State-changing requests (POST, PUT, DELETE) require a valid CSRF token using `CookieCsrfTokenRepository.withHttpOnlyFalse()` and the `X-XSRF-TOKEN` header.
5. **SQL Injection Defense**: Built entirely on Spring Data JPA parameterized queries and method derivations—no raw string concatenations.
6. **XSS Protection**: User-provided content is sanitized before rendering in the DOM using `escapeHtml()`.
