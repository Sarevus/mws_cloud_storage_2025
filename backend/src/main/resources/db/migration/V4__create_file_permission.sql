-- Таблица для прав доступа к файлам
CREATE TABLE file_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'READER')),
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    taken_back_at TIMESTAMP,

    -- Обычное уникальное ограничение (для всех записей)
    CONSTRAINT unique_file_user UNIQUE (file_id, user_id)
);

-- Частичный уникальный индекс только для активных записей
CREATE UNIQUE INDEX unique_active_permission
ON file_permissions (file_id, user_id)
WHERE taken_back_at IS NULL;

-- Индексы для ускорения поиска
CREATE INDEX idx_file_permissions_file_id ON file_permissions(file_id);
CREATE INDEX idx_file_permissions_user_id ON file_permissions(user_id);
CREATE INDEX idx_file_permissions_owner_id ON file_permissions(owner_id);
CREATE INDEX idx_file_permissions_taken_back_at ON file_permissions(taken_back_at);