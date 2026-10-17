# Developer Snippet Manager

A web application to save, organize, and search code snippets. Built with Java 17, Spring Boot, Spring Data JPA, MySQL, Spring Security (BCrypt password hashing and session-based authentication), and Vanilla HTML/CSS/JavaScript.

## Project Structure

```
snippet-manager/
├── backend/
│   ├── pom.xml                                   ← Maven configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/snippet/
│   │   │   │   ├── config/
│   │   │   │   │   └── SecurityConfig.java       ← Spring Security filter chain, BCrypt, CSRF, sessions
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java       ← Register, login, status, CSRF endpoints
│   │   │   │   │   └── SnippetController.java    ← User-scoped snippet CRUD & search
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Snippet.java              ← JPA entity for MySQL 'snippets' table
│   │   │   │   │   └── User.java                 ← JPA entity for MySQL 'users' table
│   │   │   │   ├── repository/
│   │   │   │   │   ├── SnippetRepository.java    ← Spring Data JPA with user-ownership queries
│   │   │   │   │   └── UserRepository.java       ← JPA repository for user lookup
│   │   │   │   ├── service/
│   │   │   │   │   ├── CustomUserDetailsService.java ← Loads user for Spring Security
│   │   │   │   │   ├── SnippetService.java       ← Business logic & ownership enforcement
│   │   │   │   │   └── UserService.java          ← Registration with BCrypt hashing
│   │   │   │   └── SnippetManagerApplication.java← Spring Boot main entry point
│   │   │   └── resources/
│   │   │       ├── application.properties        ← MySQL connection for snippetdb
│   │   │       └── static/                       ← Frontend files served directly by Spring Boot
│   │   │           ├── index.html
│   │   │           ├── script.js
│   │   │           └── style.css
│   │   └── test/
│   │       ├── java/com/snippet/
│   │       │   └── SnippetSecurityAndCrudIntegrationTest.java ← Automated integration test suite
│   │       └── resources/
│   │           └── application.properties        ← Test configuration pointing to snippetdb_test
└── README.md
```

---

## Prerequisites

- **Java 17+**
- **Maven**
- **MySQL Server** running on port 3306

---

## Database Setup

1. Start your local MySQL service:
   ```bash
   brew services start mysql    # macOS
   # OR: sudo systemctl start mysql  # Linux
   ```

2. Create the application database and the test database:
   ```bash
   mysql -u root -e "CREATE DATABASE IF NOT EXISTS snippetdb;"
   mysql -u root -e "CREATE DATABASE IF NOT EXISTS snippetdb_test;"
   ```

3. Spring Data JPA with Hibernate automatically creates and updates the tables (`users`, `snippets`) upon startup (`spring.jpa.hibernate.ddl-auto=update`).

---

## Running the Application

### 1. Run the Backend
```bash
cd backend
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

### 2. Access the Frontend
Open `http://localhost:8080` in your web browser. The frontend is served directly by Spring Boot from `src/main/resources/static/`.

---

## Running Automated Tests

The integration test suite connects to the separate `snippetdb_test` database configured in `src/test/resources/application.properties` to ensure application data in `snippetdb` is not modified during testing.

Make sure `snippetdb_test` is created before running tests:
```bash
cd backend
mvn test
```

---

## API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | Register a new user | Public (CSRF protected) |
| POST | `/api/auth/login` | Login and establish session | Public (CSRF protected) |
| POST | `/api/auth/logout` | Invalidate session & clear cookies | Authenticated (CSRF protected) |
| GET | `/api/auth/status` | Check current authenticated user | Public |
| GET | `/api/auth/csrf` | Get CSRF token for state-changing requests | Public |

### Snippets (`/api/snippets`)
*All snippet endpoints require authentication. Snippets are strictly isolated to the authenticated user.*

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

## Basic Security Measures

1. **BCrypt Password Hashing**: Passwords are saved as salted BCrypt hashes. Plain-text passwords are never stored in the database.
2. **Session-Based Authentication**: Standard HTTP session management using the `JSESSIONID` cookie with `HttpOnly`.
3. **User-Scoped Ownership**: The server determines the authenticated user from the session and restricts snippet queries to the current user via `findByIdAndUser(id, user)`.
4. **ID Injection Prevention**: In snippet creation, the ID is explicitly set to `null` on the server so clients cannot force an existing ID.
5. **CSRF Protection**: Enabled with `CookieCsrfTokenRepository.withHttpOnlyFalse()`. The frontend JavaScript sends the `X-XSRF-TOKEN` header on state-changing requests.
6. **SQL Parameterization**: Spring Data JPA repository methods automatically use parameterized queries, preventing SQL injection.
7. **Basic XSS Sanitization**: User-supplied values are escaped before rendering in the DOM.
