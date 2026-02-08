CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    link VARCHAR(500) NOT NULL,  -- Указали длину!
    category VARCHAR(50) DEFAULT 'general'
);

ALTER TABLE files
ADD CONSTRAINT fk_files_users_id
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_files_category ON files(category);
CREATE INDEX IF NOT EXISTS idx_files_user_category ON files(user_id, category);
CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);