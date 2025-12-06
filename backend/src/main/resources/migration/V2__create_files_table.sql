CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    link VARCHAR NOT NULL
);

ALTER TABLE files
ADD CONSTRAINT fk_files_users_id
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;