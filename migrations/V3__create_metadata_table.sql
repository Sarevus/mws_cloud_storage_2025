CREATE TABLE metadata (
    file_id UUID PRIMARY KEY REFERENCES files(id) ON DELETE CASCADE,
    original_name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    is_public BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_metadata_mime_type ON metadata(mime_type);