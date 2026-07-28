CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() is STABLE, not IMMUTABLE, so it cannot be used in a generated
-- column. The two-argument form with an explicit dictionary is deterministic
-- and can safely be wrapped.
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT AS
$$ SELECT public.unaccent('public.unaccent', $1) $$;

-- "Jack O'Hara" -> jackohara
-- "O’Hara" (U+2019) -> ohara
-- "José Álvarez" -> josealvarez
-- "ACC-889 134" -> acc889134
-- "john.doe@neviswealth.com" -> johndoeneviswealthcom
-- lower(... COLLATE "C") pins case-folding to the C locale so it never depends on
-- the database's default collation, mirroring toLowerCase(Locale.ROOT) on the Java
-- side. Without it, a Turkish-collation database folds 'I' to the dotless 'ı', which
-- the [^a-z0-9] strip then drops — silently diverging from the Java normaliser.
CREATE OR REPLACE FUNCTION search_normalize(text)
RETURNS text LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$ SELECT regexp_replace(
       lower(immutable_unaccent(coalesce($1, '')) COLLATE "C"),
       '[^a-z0-9]', '', 'g') $$;
