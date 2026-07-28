-- Quick summary of the document, generated once at creation and stored (never during
-- search). Populated by an LLM when configured, or by an extractive fallback otherwise
-- see com.nevis.search.llm.
ALTER TABLE documents ADD COLUMN summary TEXT;

-- Preserve the pre-summary search response for existing rows by backfilling the same
-- whitespace-collapsed, capped snippet that SearchService used to compute at read time.
UPDATE documents
SET summary = CASE
    WHEN length(regexp_replace(btrim(content), '\s+', ' ', 'g')) <= 160
        THEN regexp_replace(btrim(content), '\s+', ' ', 'g')
    ELSE rtrim(substring(regexp_replace(btrim(content), '\s+', ' ', 'g') from 1 for 160)) || '…'
END
WHERE summary IS NULL;
