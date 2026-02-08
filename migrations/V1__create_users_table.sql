CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    phoneNumber VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL
);