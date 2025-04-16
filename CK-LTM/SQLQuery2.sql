CREATE DATABASE chat_data;

USE chat_data;

CREATE TABLE text_messages (
    id INT IDENTITY(1,1) PRIMARY KEY,
    encrypted_text TEXT,
    decrypted_text TEXT,
    key_value INT,
    letter_frequencies TEXT,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE received_files (
    id INT IDENTITY(1,1) PRIMARY KEY,
    filename VARCHAR(255),
    filepath TEXT,
    size BIGINT,
    received_at DATETIME DEFAULT GETDATE()
);
