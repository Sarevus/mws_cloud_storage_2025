CREATE TABLE IF NOT EXISTS folder_manager (
    folder_id UUID NOT NULL,
    file_id UUID NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(255),
    PRIMARY KEY (folder_id, file_id),
    CONSTRAINT fk_folder_manager_folder FOREIGN KEY (folder_id)
        REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_manager_file FOREIGN KEY (file_id)
        REFERENCES files(id) ON DELETE CASCADE
);

-- Индексы для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_folder_manager_folder_id ON folder_manager(folder_id);
CREATE INDEX IF NOT EXISTS idx_folder_manager_file_id ON folder_manager(file_id);
CREATE INDEX IF NOT EXISTS idx_folder_manager_added_at ON folder_manager(added_at);