# Developer Snippet Manager

A simple web app to save, organize, and search code snippets. Built with Spring Boot, MongoDB, and plain HTML/CSS/JS.

## Project Structure

```
snippet-manager/
├── backend/
│   ├── src/main/java/com/snippet/
│   │   ├── controller/SnippetController.java   ← 6 REST endpoints
│   │   ├── entity/Snippet.java                 ← MongoDB document
│   │   ├── repository/SnippetRepository.java   ← DB queries
│   │   ├── service/SnippetService.java         ← Business logic
│   │   └── SnippetManagerApplication.java      ← App entry point
│   └── src/main/resources/application.properties
├── frontend/
│   ├── index.html   ← Single page UI
│   ├── style.css    ← Basic styling
│   └── script.js    ← Fetch API calls
└── README.md
```

## Prerequisites

- Java 17+
- Maven
- MongoDB running locally on port 27017

## Setup & Run

### 1. Start MongoDB

Make sure MongoDB is running locally:

```bash
mongod
```

MongoDB will use the `snippetdb` database automatically (created on first insert).

### 2. Run the Backend

```bash
cd backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### 3. Open the Frontend

Open `frontend/index.html` directly in your browser. No server needed for the frontend.

> **Note:** Since the frontend opens as a local file, make sure the backend is running first. The frontend calls `http://localhost:8080`.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/snippets` | Get all snippets |
| GET | `/api/snippets/{id}` | Get snippet by ID |
| POST | `/api/snippets` | Create new snippet |
| PUT | `/api/snippets/{id}` | Update snippet |
| DELETE | `/api/snippets/{id}` | Delete snippet |
| GET | `/api/snippets/search?language=java` | Search by language |
| GET | `/api/snippets/search?title=loop` | Search by title |
| GET | `/api/snippets/search?language=java&title=loop` | Search by both |

### Example POST Request Body

```json
{
  "title": "Reverse a String",
  "language": "Java",
  "description": "Simple string reversal using StringBuilder",
  "code": "String reversed = new StringBuilder(str).reverse().toString();"
}
```

## How It Works

1. **Entity** (`Snippet.java`) — defines the MongoDB document with fields: `id`, `title`, `code`, `language`, `description`, `createdAt`
2. **Repository** (`SnippetRepository.java`) — extends `MongoRepository`, has custom query methods for search
3. **Service** (`SnippetService.java`) — handles CRUD logic, sets `createdAt` on creation
4. **Controller** (`SnippetController.java`) — maps HTTP requests to service methods, returns JSON responses
5. **Frontend** (`script.js`) — uses `fetch()` to call the API, renders snippet cards with Edit/Delete/Copy buttons

## MongoDB Collection

The app uses the `snippetdb` database and a `snippets` collection. There is an index on the `language` field for faster language-based searches.
