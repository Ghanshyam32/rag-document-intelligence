# Empty Stack — RAG Document Intelligence Backend

A production-ready REST API that enables natural language Q&A over uploaded documents using Retrieval-Augmented Generation (RAG).

Upload a PDF or TXT file → ask questions in plain English → get answers with source citations.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| AI Framework | Spring AI 1.1.1 |
| Embedding Model | Gemini `gemini-embedding-001` (3072 dims) |
| LLM | Gemini 2.0 Flash |
| Vector Store | PostgreSQL + pgvector |
| PDF Parsing | Apache PDFBox 3.x |
| DB Migrations | Flyway |
| Security | Spring Security (JWT — WIP) |
| Containerization | Docker + Docker Compose |

---

## How RAG Works

```
User uploads PDF
  → chunked into 500-token pieces
  → each chunk embedded into a vector

User asks a question
  → question embedded
  → cosine similarity search in pgvector
  → top 5 matching chunks injected into Gemini prompt
  → answer returned with source citations
```

---

## Project Structure

```
src/main/java/com/ghanshyam/empty_stack/
├── controller/
│   ├── DocumentController.java    # upload, list, delete endpoints
│   └── ChatController.java        # ask, clear history endpoints
├── service/
│   ├── DocumentService.java       # orchestrates upload pipeline
│   ├── ChunkingService.java       # PDF parsing + text chunking
│   ├── EmbeddingService.java      # Gemini embedding via REST
│   └── ChatService.java           # RAG Q&A + conversational memory
├── model/
│   ├── Document.java
│   └── DocumentChunk.java
├── repository/
│   ├── DocumentRepository.java
│   └── DocumentChunkRepository.java
└── dto/
    ├── ChatRequest.java
    ├── ChatResponse.java
    └── ChunkSearchResult.java
```

---

## API Endpoints

### Document Management

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/documents/upload` | Upload and index a PDF or TXT file |
| `GET` | `/api/documents` | List all indexed documents |
| `GET` | `/api/documents/{id}` | Get document details |
| `DELETE` | `/api/documents/{id}` | Delete document and its embeddings |

### Chat / Q&A

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/chat/ask` | Ask a question, get answer + sources |
| `DELETE` | `/api/chat/history/{id}` | Clear conversation memory |

---

## Setup & Run

### Prerequisites

- Java 17+
- Docker Desktop
- Gemini API key (free at [aistudio.google.com](https://aistudio.google.com))

### 1. Clone the repo

```bash
git clone https://github.com/Ghanshyam32/rag-document-intelligence.git
cd rag-document-intelligence
```

### 2. Configure

```bash
copy src\main\resources\application.yml.example src\main\resources\application.yml
# Edit application.yml and add your Gemini API key
```

### 3. Start PostgreSQL with pgvector

```bash
docker compose up -d
```

### 4. Run the app

```bash
mvn spring-boot:run
```

---

## Example Usage

**Upload a document:**

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@handbook.pdf"
```

**Ask a question:**

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the leave policy for new employees?"}'
```

**Response:**

```json
{
  "answer": "New employees are entitled to 12 days of casual leave per year (from handbook.pdf).",
  "conversationId": "uuid-here",
  "sources": [
    {
      "documentName": "handbook.pdf",
      "chunkIndex": 14,
      "excerpt": "New employees during their probation period..."
    }
  ]
}
```

---

## Chunking Strategy

- **Chunk size:** 500 tokens
- **Overlap:** 50 tokens — prevents context loss at chunk boundaries
- Split on whitespace; overlap ensures sentences aren't cut mid-thought

---

## Roadmap

- [ ] JWT authentication
- [ ] Global exception handling
- [ ] Full Docker Compose (app + postgres)
- [ ] Streaming responses (SSE)
- [ ] Redis for persistent conversation memory