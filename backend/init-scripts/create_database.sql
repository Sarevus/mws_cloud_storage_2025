CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    phoneNumber VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL
);

CREATE TABLE files (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    link VARCHAR NOT NULL,

    CONSTRAINT fk_files_users_id
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);