-- V1__create_tables.sql
-- Flyway migration: создание таблиц users и files

-- Создание таблицы users
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phonenumber VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL
    );

-- Создание таблицы files
CREATE TABLE IF NOT EXISTS files (
                                     id UUID PRIMARY KEY,
                                     user_id UUID NOT NULL,
                                     s3_key VARCHAR(500) NOT NULL UNIQUE,
    original_name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Индексы для ускорения поиска
CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);
CREATE INDEX IF NOT EXISTS idx_files_s3_key ON files(s3_key);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);