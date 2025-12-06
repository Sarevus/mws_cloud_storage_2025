CREATE TABLE metadata (
    file_id UUID PRIMARY KEY REFERENCES files(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    description TEXT,
    tags VARCHAR(500),
    is_public BOOLEAN DEFAULT FALSE
);