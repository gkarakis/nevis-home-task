# Nevis Search API

Clients and their documents, returned as **one ranked list**. Document relevance
fuses a semantic (vector) and a lexical (full-text + identifier) signal with
reciprocal rank fusion; documents are ordered against clients with an explicit,
stated tier rule rather than an invented cross-type score.

- **Java 21 · Spring Boot 3.3 · Maven**
- **PostgreSQL 16 + pgvector** (`pg_trgm`, `unaccent`) — the single system of record
- **Local, in-process embeddings** — all-MiniLM-L6-v2 ONNX via Spring AI, 384 dims, no API key, no network egress
- **Flyway** migrations · **springdoc** Swagger UI · **Testcontainers** golden tests

---

## 1. Quick start

Requires Docker only. On a clean checkout:

```bash
docker compose up --build
```

This starts Postgres (pgvector) and the app, runs the Flyway migrations, and seeds
demo data (the `demo` profile). No environment variables, no API keys, no manual
steps. Swagger UI: <http://localhost:8080/swagger-ui.html>.

The model and tokenizer are baked into the image at **build** time, so a cold
container needs no network access to serve its first request.

Three curl commands reproducing the brief's own examples:

```bash
# 1. A client found by a substring inside their email ("NevisWealth" in the domain)
curl 'http://localhost:8080/search?q=NevisWealth'

# 2. Semantic + lexical document search. The doc matched by BOTH channels ranks
#    first (RRF); the off-topic portfolio note is dropped by the relevance floor.
curl 'http://localhost:8080/search?q=address+proof'

# 3. An off-topic query clears no floor and returns an empty array (HTTP 200)
curl 'http://localhost:8080/search?q=elephant+breeding+habits'
```

> If host port 5432 is already in use, publish Postgres elsewhere, e.g.
> `docker compose run` with an override mapping `"5433:5432"`. The app reaches
> Postgres over the compose network regardless of the host mapping.

### Running without Docker

```bash
# Postgres with pgvector must be available at localhost:5432 (db/user/pass: nevis)
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

---

## 2. API

Base URL `http://localhost:8080`. All request and response bodies are JSON in
`snake_case`; timestamps are RFC 3339 in UTC. **Null fields are omitted** from
responses (`default-property-inclusion: non_null`), so an absent `description` or a
document owner's `email` simply does not appear on the wire.

| Method & path | Purpose | Success |
|---|---|---|
| `POST /clients` | Create a client | `201` + `Location` |
| `GET /clients` | List all clients | `200` |
| `POST /clients/{clientId}/documents` | Add a document to a client | `201` + `Location` |
| `GET /search` | Search clients + documents as one ranked list | `200` |

### 2.0 Error shape

Every 4xx/5xx uses **one shape**. `details` lists per-field violations on validation
failures and is `[]` otherwise:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "timestamp": "2026-07-23T09:14:22Z",
  "details": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

| `code` | HTTP | Raised when |
|---|---|---|
| `VALIDATION_FAILED` | `400` | A request body fails bean validation (`details` names the fields) |
| `INVALID_QUERY` | `400` | `q` is missing, or empty/too short after normalisation |
| `CLIENT_NOT_FOUND` | `404` | `clientId` does not exist |
| `EMAIL_ALREADY_EXISTS` | `409` | Another client already uses that email (unique on `lower(email)`) |
| `EMBEDDING_UNAVAILABLE` | `503` | The embedding model failed; **nothing was written** |

---

### 2.1 `POST /clients` — create a client

**Request body**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `first_name` | string | yes | non-blank, ≤ 255 |
| `last_name` | string | yes | non-blank, ≤ 255 |
| `email` | string | yes | non-blank, valid email, ≤ 320, unique (case-insensitive) |
| `description` | string | no | ≤ 5000 |
| `social_links` | string[] | no | each must be an `http(s)://…` URL |

```bash
curl -X POST http://localhost:8080/clients \
  -H 'Content-Type: application/json' \
  -d '{
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@neviswealth.com",
    "description": "Long-standing wealth client",
    "social_links": ["https://linkedin.com/in/johndoe"]
  }'
```

**`201 Created`** — `Location: /clients/{id}`. Body:

```json
{
  "id": "4df7a1e0-1c2b-4f3a-9a11-8b7c6d5e4f30",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@neviswealth.com",
  "description": "Long-standing wealth client",
  "social_links": ["https://linkedin.com/in/johndoe"],
  "created_at": "2026-01-14T10:02:11Z"
}
```

**Errors:** `400 VALIDATION_FAILED` · `409 EMAIL_ALREADY_EXISTS`.

---

### 2.2 `GET /clients` — list clients

No parameters. **`200 OK`** — a top-level JSON **array** of the object shown in §2.1
(empty array when there are no clients).

```bash
curl http://localhost:8080/clients
```

---

### 2.3 `POST /clients/{clientId}/documents` — add a document

**Path parameter:** `clientId` (UUID of an existing client).

**Request body**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `title` | string | yes | non-blank, ≤ 500 |
| `content` | string | yes | non-blank, ≤ 50 000 — **pre-extracted plain text** (no file upload) |

```bash
curl -X POST http://localhost:8080/clients/4df7a1e0-1c2b-4f3a-9a11-8b7c6d5e4f30/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "John'\''s utility bill",
    "content": "Electricity bill for 123 Main Street dated 14 Jan 2026, account ACC-889134…"
  }'
```

**`201 Created`** — `Location: /clients/{clientId}/documents/{id}`. Body:

```json
{
  "id": "9c1f2b3a-4d5e-6f70-8192-a3b4c5d6e7f8",
  "client_id": "4df7a1e0-1c2b-4f3a-9a11-8b7c6d5e4f30",
  "title": "John's utility bill",
  "chunk_count": 1,
  "created_at": "2026-01-14T10:02:11Z"
}
```

`chunk_count` is how many ~800-char chunks the content was split into and embedded
(see §4). Embedding happens **before** the write transaction, so a `201` guarantees
every chunk is embedded and immediately searchable.

**Errors:** `400 VALIDATION_FAILED` · `404 CLIENT_NOT_FOUND` · `503 EMBEDDING_UNAVAILABLE`.

---

### 2.4 `GET /search` — search clients and documents

**Query parameters**

| Param | Type | Required | Default | Notes |
|---|---|---|---|---|
| `q` | string | yes | — | The search text. Normalised (lowercase, accent-fold, strip non-alphanumerics) before matching. |
| `limit` | integer | no | `20` | Max results. Clamped to `[1, 100]` — an over-large value is **clamped, not rejected**. |

```bash
curl 'http://localhost:8080/search?q=address+proof&limit=20'
```

**`200 OK`** — a **top-level JSON array** (no envelope), empty `[]` when nothing
clears the relevance floors. Each element is a discriminated union on `type`:

| Field | Type | Present for | Meaning |
|---|---|---|---|
| `type` | `CLIENT` \| `DOCUMENT` | all | Which kind of hit this is |
| `id` | UUID | all | Client id or document id |
| `score` | number | all | Relevance **within its own type** (cosine sim. for documents, trigram sim. for clients) — see note below |
| `matched_by` | Channel[] | all | Any of `SEMANTIC`, `LEXICAL` (documents) or `DIRECT`, `FUZZY` (clients) |
| `client` | object | all | The client — for a `DOCUMENT` it is the owner, with `email`/`description` omitted |
| `client.id / first_name / last_name` | UUID / string / string | all | — |
| `client.email / description` | string | `CLIENT` hits only | Omitted (null) on document owners |
| `document` | object | `DOCUMENT` only | The matched document |
| `document.id / title / created_at` | UUID / string / timestamp | `DOCUMENT` | — |
| `document.snippet` | string | `DOCUMENT` | **Quick summary of the document** — a whitespace-collapsed leading slice of the content (≤ 160 chars, ellipsised) |

Example — a document matched by **both** channels (RRF), followed by a directly
matched client:

```json
[
  {
    "type": "DOCUMENT",
    "id": "9c1f2b3a-4d5e-6f70-8192-a3b4c5d6e7f8",
    "score": 0.71,
    "matched_by": ["SEMANTIC", "LEXICAL"],
    "client": {
      "id": "4df7a1e0-1c2b-4f3a-9a11-8b7c6d5e4f30",
      "first_name": "John",
      "last_name": "Doe"
    },
    "document": {
      "id": "9c1f2b3a-4d5e-6f70-8192-a3b4c5d6e7f8",
      "title": "John's utility bill",
      "snippet": "Electricity bill for 123 Main Street dated 14 Jan 2026, account ACC-889134…",
      "created_at": "2026-01-14T10:02:11Z"
    }
  },
  {
    "type": "CLIENT",
    "id": "4df7a1e0-1c2b-4f3a-9a11-8b7c6d5e4f30",
    "score": 0.83,
    "matched_by": ["DIRECT"],
    "client": {
      "id": "4df7a1e0-1c2b-4f3a-9a11-8b7c6d5e4f30",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@neviswealth.com",
      "description": "Long-standing wealth client"
    }
  }
]
```

`score` is relevance **within a result's own type**. Because array order follows the
tier rule in §3 (not a global score), scores are **not** globally monotonic.

**Errors:** `400 INVALID_QUERY` when `q` is missing or normalises to fewer than two
characters.

---

## 3. Search design

Three queries, then two steps.

**Query A — clients.** Substring and trigram-similarity match over a normalised
`search_blob` (name + email + description), served by a GIN trigram index. A
`direct_match` flag separates certain substring hits from fuzzy ones — and is
computed over **name + email only**, since description is descriptive text, not an
identity signal. A client matched solely on their description is retrieved (via the
`search_blob` index) but flagged fuzzy, so it sorts below documents, not above them.

**Query B1 — documents, semantic.** A document's score is its best-scoring chunk:
`MAX(1 - (embedding <=> query))`, i.e. cosine similarity. A `HAVING` floor drops
near-misses before fusion.

**Query B2 — documents, lexical.** `websearch_to_tsquery` full-text (title weighted
above body) plus a **digit-guarded** identifier path: a query containing a digit
also substring-matches the normalised `doc_blob`, so `ACC 889134` finds a document
containing `ACC-889134`. The digit guard keeps this path from firing on ordinary
words (`bill` must not match "billing", "billion", "Hillbilly").

**Step 1 — fuse the two document signals (RRF).** Cosine similarity and
`ts_rank_cd` are not comparable numbers, so we fuse by rank position:
`fused(doc) = Σ 1 / (k + rank)`, `k = 60`. A document found by **both** signals
outranks one found by a single signal — agreement between independent methods is
real evidence.

**Step 2 — order documents against clients (explicit rule).** There is no
principled way to compare "this client's email contains your query" with "this
document is semantically about your query", so cross-type ordering is a stated
product decision, not a calculation:

1. **Clients matched directly** (substring on name or email) — a direct identity match is a near-certain intent signal.
2. **Documents**, ordered by fused relevance.
3. **Clients matched only fuzzily** (trigram similarity above the floor).

*An advisor typing a client's name or email almost always wants that client;
everything else is relevance-ordered.* If the priority is wrong for Nevis, it is a
one-line change in `ResultOrdering`.

**Floors are not optional.** Vector search always returns a nearest neighbour, so
without a floor every off-topic query returns confident nonsense. Floors are
model-specific and were **tuned empirically** against the seed data:

| Property | Value | Meaning |
|---|---|---|
| `semantic-floor` | `0.20` | cosine similarity — keeps `address proof` → utility bill (~0.24), drops the portfolio note (~0.06) and off-topic queries |
| `client-floor` | `0.30` | trigram similarity for fuzzy client matches |
| `rrf-k` | `60` | RRF constant |
| `channel-depth` | `50` | rows per query before fusion |

They would need re-tuning with a different embedding model.

### Normalisation (why `O'Hara`, `O’Hara`, `OHara`, `o hara` all match)

Client and identifier matching normalise once on write and once on read: lowercase,
strip accents, remove every non-alphanumeric character. This is implemented **twice**
— as the `search_normalize()` SQL function (used by `STORED` generated columns) and
as `SearchNormalizer` in Java (applied to the query before it reaches SQL). Drift
between the two is silent and returns zero results, so both are asserted against a
**shared fixture list** (`NormalizationFixtures`) — the Java unit test and an
integration test that calls the real SQL function.

---

## 4. Design decisions

- **Postgres over Elasticsearch.** Elasticsearch offers richer lexical relevance,
  but it cannot replace Postgres as the system of record — so it adds a second
  datastore and a dual-write consistency problem (outbox/CDC). With pgvector, a
  document and its chunk vectors commit in one transaction: if the row exists, it is
  searchable. Worth revisiting at ~10M+ documents, or if faceting/aggregations
  become requirements.
- **Local model over hosted.** `docker compose up` must produce a working API with
  no API key. A local model also means client documents never leave the deployment
  — meaningful for financial data. Switching providers would change the vector
  dimension and force a migration + full re-index, so it is a deployment concern,
  not a runtime abstraction; hence one `EmbeddingService` interface, one
  implementation, one test double — no provider framework.
- **Synchronous embedding for a consistency invariant.** Chunking and embedding
  happen *before* the write transaction opens (holding a pooled connection across
  model inference would exhaust the pool). `DocumentWriter.save(...)` then commits
  the document and all chunks atomically, so **document exists ⇒ its chunks are
  embedded** always holds. If embedding fails, nothing is written and the request
  returns `503`.
- **Chunking.** `content` allows 50,000 characters but the model's input window is a
  few hundred wordpieces — embedding a long document whole would silently truncate
  it. Content is split into ~800-char chunks; each chunk's embedding text is
  prefixed with the document title, so a query for `passport` finds a "Passport
  Copy" whose body barely uses the word (verified: score ~0.59). A document's
  semantic relevance is its best-scoring chunk; chunks are never exposed in the API.
- **Explicit ordering rule instead of a fabricated score comparison** — see §3.
- **Blocking, sequential search over reactive WebFlux.** The three search queries
  run in order on one thread, and reactive parallelism was considered and rejected.
  WebFlux over blocking JPA/JDBC is an anti-pattern — real non-blocking I/O needs
  R2DBC, and the embedding model is CPU-bound inference regardless — so it would buy
  reactive's complexity with none of its throughput. Parallelising the queries is
  also fenced off by design: the read runs in one `@Transactional` unit bound to a
  single thread-unsafe connection, and the dominant cost is the embedding, not the
  three indexed queries (and `searchSemantic` can't start until the vector exists
  anyway). If latency ever mattered, the move would be to measure first, then overlap
  the embedding with the two non-vector queries via separate read connections — not a
  WebFlux rewrite.
- **Native SQL for search.** The queries need `<=>`, `similarity()` and
  `ts_rank_cd`, none of which JPQL expresses; CRUD uses Spring Data JPA.
- **No vector index.** An exact scan gives perfect recall and is fast at this scale.
  Add HNSW (`vector_cosine_ops`) only when measured volume requires it, accepting
  approximate recall.

---

## 5. Clarifications received

Six questions were confirmed before starting; they are the contract this
implementation is built against.

| Question | Answer | Consequence |
|---|---|---|
| Is `content` pre-extracted plain text? | Confirmed. No file upload or parsing. | No multipart endpoints. |
| Exact-identifier matching in documents? | Semantic is the minimum, hybrid is "definitely a plus". | Built the lexical document channel. |
| Compound queries like "address proof of John Doe"? | Not required — keep it simple. | No query parser (see Limitations). |
| Grouped or single ranked list? | **Single ranked list.** | Cross-type ordering decided and defended (§3). |
| Auth and tenant isolation? | Out of scope. | No security layer. |
| Scale and latency? | Small dataset accepted. | Exact vector scan, no ANN index. |

---

## 6. Assumptions

- **Email is unique.** An email identifies a client account — enforced by a unique
  index on `lower(email)`; `409` on collision.
- **Social links are stored but not searched.** The requirement names email, name
  and description only.
- **Content is plain text**, split into fixed-size chunks before embedding so long
  documents are not silently truncated.

---

## 7. Limitations

- **No compound-query parsing.** For `"address proof of John Doe"`, the client query
  surfaces John Doe and the document queries surface address-proof documents from
  *all* clients — the name acts as a signal, not a filter. The fix (a parser
  extracting `{client, concept}` and scoping documents by `client_id`) is future work.
- **No pagination** (no cursor/offset), **no update/delete/re-index** endpoints,
  **no caching**, **no auth/tenant isolation** — all out of scope per §5.
- **Latin-script languages only.** Search is tuned for English and accented Latin
  text; non-Latin scripts (CJK, Cyrillic, Arabic, Greek, …) are effectively unsupported.
  Three causes, one per channel: (1) `search_normalize()` folds accents and strips
  everything outside `[a-z0-9]`, so `José Álvarez` matches but `Владимир` / `李伟` reduce
  to nothing — client and identifier lookups can't see them; (2) full-text search is
  pinned to the `english` config (`to_tsvector`/`websearch_to_tsquery`), so other
  languages get wrong stemming and stopwords; (3) `all-MiniLM-L6-v2` is English-trained,
  so non-English semantic recall is weak. The fixes — Unicode-aware normalisation
  (NFKC + case-folding, keeping all-script letters), a language-parameterised or
  `simple` FTS config, and a multilingual embedding model (the last forces a vector-
  dimension migration + full re-index + floor re-tuning) — are future work.
- **Floors are tuned to this model** and would need re-tuning for another.
- **Cross-type order is a product rule**, not a computed ranking (by design, §3).
- **Summary is extractive, not abstractive.** `document.snippet` is a leading slice
  of content (see §8), not an LLM-written summary. This satisfies the brief's
  *optional* "quick summary of document content" cheaply; a real summary is §8.

---

## 8. Document summary (the "optional quick summary")

The brief lists an **optional** *"quick summary of document content"*. It is served
today by `document.snippet` in the search response (§2.4): a whitespace-collapsed
leading slice of the content, ≤ 160 chars. That is honest to call a summary only in
the weakest sense — it is **extractive by position** and does not understand the
document. A genuine summary needs an LLM. We shipped the slice and did **not** build
the LLM path; the reasons and the intended shape are below.

### Proposed solution (not built)

One seam, generation once at create time, an LLM behind it:

1. **Schema** — add a nullable `documents.summary TEXT` column (new migration).
2. **Interface** — `Summarizer { String summarize(String title, String content); }`,
   mirroring the single-implementation `EmbeddingService`. Two implementations: an
   **LLM** one (the real summary) and a **leading-text** one (extractive fallback for
   deployments with no model configured).
3. **Write path** — call the summarizer in `DocumentService.create()`, in the same
   pre-transaction block that already embeds, so no pooled DB connection is held
   across inference. `DocumentWriter.save(...)` persists `summary` atomically with the
   row — the same invariant the embeddings get (*document exists ⇒ summary present*).
4. **Read path** — `SearchService` returns the stored `summary`; **summarization never
   runs during search.** The `snippet` field is renamed `summary`; its wire shape is
   unchanged, so no client breaks.

The value of this seam is *not* better summaries on its own — leading-text stored at
write time is the same quality tier as the leading-slice computed at query time. Its
value is that swapping the extractive impl for the LLM impl is then a **one-class
change with no API change**.

### Why Option C (the LLM) was not implemented

- **It breaks the repo's stated no-egress guarantee.** §4 makes local, in-process
  inference a design principle precisely because these are **financial documents** —
  *"client documents never leave the deployment."* A hosted LLM (e.g. Claude) sends
  document content off-box and requires the DPA / data-residency / retention review
  flagged below — a compliance decision, not a code change, and not ours to make for
  Nevis in a take-home.
- **A local generative model preserves egress but is disproportionate here.** It keeps
  data on-box but adds a much larger image, meaningful per-document latency, and infra
  weight — a heavy dependency for an *optional* field on a small dataset, and a
  weaker summary than a hosted model would give.
- **The requirement is explicitly optional**, so the right call was to satisfy the
  field cheaply and deterministically (zero new dependencies, no external calls) and
  leave the seam for whichever summarizer Nevis's privacy posture allows — rather than
  bake a provider and its data-flow implications into the submission.

---

## 9. Production considerations

- **Tenant scoping** enforced at the data-access layer.
- **Async indexing**: persist documents `PENDING` and index out-of-band with retries
  and a dead-letter queue, instead of synchronous embedding.
- **HNSW index** on `document_chunks.embedding` when volume requires it.
- **DPA, data residency and retention** review before adopting any external
  embedding **or summarization** provider (see §8).

---

## Testing

```bash
./mvnw test      # unit tests only (fast; no Docker required)
./mvnw verify    # + integration golden cases (Testcontainers → real Postgres + model)
```

Unit tests cover `DocumentRankFusion` (a doc in both lists outranks a higher-placed
doc in one; stable ordering; empty lists), `ResultOrdering` (tier precedence, limit,
determinism), `SearchNormalizer` (shared fixtures), `TextChunker`, `DocumentService`
(unknown client throws before any embedding call; embedding failure persists
nothing), and `ClientController` (201/400/409 via MockMvc).

The Cucumber integration suite (`CucumberIntegrationTest`, with scenarios tagged
`integration`) runs Flyway against a real `pgvector/pgvector:pg16` container — so
the migrations, the `IMMUTABLE` function and the generated columns are under test
— and asserts the golden cases against the seed data using the **real** embedding
model:

| Query | Expected |
|---|---|
| `NevisWealth` | Client `john.doe@neviswealth.com` |
| `NEVISWEALTH` / `neviswealth` | Identical result set |
| `address proof` | Utility bill ranks above the portfolio note (which the floor drops) |
| `John Doe` | Matches across `first_name` + `last_name` |
| `O'Hara`, `O’Hara`, `OHara`, `o hara` | All four return Jack O'Hara |
| `Jose Alvarez` | Returns José Álvarez |
| `ACC 889134` | Returns the document containing `ACC-889134` |
| `elephant breeding habits` | `200` with `[]` |

> **Platform note.** The integration suite is skipped automatically on **macOS
> Intel (osx-x86_64)**, where DJL ships no working tokenizer native library
> (`Unexpected flavor: cpu`). It runs on the `linux/amd64` Docker target, on Linux
> CI, and on Apple Silicon. On any host you can reproduce the golden cases directly
> against the running container with the curl commands in §1.
