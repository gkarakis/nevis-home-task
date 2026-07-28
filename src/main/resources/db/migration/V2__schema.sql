CREATE TABLE clients (
    id           UUID PRIMARY KEY,
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(320) NOT NULL,
    description  TEXT,
    social_links TEXT[] NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- name fields concatenated before normalising, so "John Doe" matches
    -- across two columns with no query-side gymnastics
    search_blob  TEXT GENERATED ALWAYS AS (
        search_normalize(first_name || ' ' || last_name || ' ' ||
                         email || ' ' || coalesce(description, ''))
    ) STORED
);

CREATE UNIQUE INDEX uq_clients_email_lower ON clients (lower(email));
CREATE INDEX idx_clients_blob_trgm
    ON clients USING GIN (search_blob gin_trgm_ops);

CREATE TABLE documents (
    id         UUID PRIMARY KEY,
    client_id  UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    title      VARCHAR(500) NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- word-level lexical search; title weighted above body
    search_tsv TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title,   '')), 'A') ||
        setweight(to_tsvector('english', coalesce(content, '')), 'B')
    ) STORED,

    -- identifier lookup only: "ACC-889134" and "ACC 889134" both -> acc889134
    doc_blob   TEXT GENERATED ALWAYS AS (
        search_normalize(title || ' ' || content)
    ) STORED
);

CREATE TABLE document_chunks (
    id          UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content     TEXT NOT NULL,
    embedding   VECTOR(384) NOT NULL,
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_documents_client   ON documents (client_id);
CREATE INDEX idx_documents_tsv      ON documents USING GIN (search_tsv);
CREATE INDEX idx_documents_blob     ON documents USING GIN (doc_blob gin_trgm_ops);
CREATE INDEX idx_chunks_document    ON document_chunks (document_id);

-- No vector index. Exact scan gives perfect recall and is fast enough at
-- this scale. Add HNSW only when measured volume requires it, accepting
-- approximate recall:
--   CREATE INDEX idx_chunks_embedding ON document_chunks
--       USING hnsw (embedding vector_cosine_ops);
