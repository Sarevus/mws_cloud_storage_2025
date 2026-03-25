-- Материализованное представление для подсчёта места пользователя
CREATE MATERIALIZED VIEW IF NOT EXISTS user_storage_usage AS
SELECT
    f.user_id,
    COALESCE(SUM(f.size), 0) AS total_bytes
FROM files f
GROUP BY f.user_id;

-- Индекс для быстрого поиска
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_storage_user_id ON user_storage_usage(user_id);

-- Функция для обновления (ПОЛНОСТЬЮ ИСПРАВЛЕННАЯ)
CREATE OR REPLACE FUNCTION refresh_user_storage_usage()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY user_storage_usage;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Триггеры для автоматического обновления - файлы
DROP TRIGGER IF EXISTS refresh_storage_on_file_insert ON files;
CREATE TRIGGER refresh_storage_on_file_insert
    AFTER INSERT ON files
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();

DROP TRIGGER IF EXISTS refresh_storage_on_file_update ON files;
CREATE TRIGGER refresh_storage_on_file_update
    AFTER UPDATE OF size ON files
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();

DROP TRIGGER IF EXISTS refresh_storage_on_file_delete ON files;
CREATE TRIGGER refresh_storage_on_file_delete
    AFTER DELETE ON files
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();


-- Триггеры для автоматического обновления - папки+файлы
DROP TRIGGER IF EXISTS refresh_storage_on_folder_manager_insert ON folder_manager;
CREATE TRIGGER refresh_storage_on_folder_manager_insert
    AFTER INSERT ON folder_manager
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();

DROP TRIGGER IF EXISTS refresh_storage_on_folder_manager_delete ON folder_manager;
CREATE TRIGGER refresh_storage_on_folder_manager_delete
    AFTER DELETE ON folder_manager
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();


-- Триггеры для автоматического обновления - папки
DROP TRIGGER IF EXISTS refresh_storage_on_folder_insert ON folders;
CREATE TRIGGER refresh_storage_on_folder_insert
    AFTER INSERT ON folders
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();

DROP TRIGGER IF EXISTS refresh_storage_on_folder_update ON folders;
CREATE TRIGGER refresh_storage_on_folder_update
    AFTER UPDATE OF size ON folders
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();

DROP TRIGGER IF EXISTS refresh_storage_on_folder_delete ON folders;
CREATE TRIGGER refresh_storage_on_folder_delete
    AFTER DELETE ON folders
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_user_storage_usage();