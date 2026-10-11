-- Run this script as a MySQL administrator once to create the persistent application database.
-- Create a separate least-privilege application user and set DB_USERNAME / DB_PASSWORD outside source control.
CREATE DATABASE IF NOT EXISTS sai_sahayadri
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE sai_sahayadri;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT users_role_allowed CHECK (role IN ('ADMIN', 'STUDENT'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS batches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_batches_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS student_profiles (
    user_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    batch_id BIGINT,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_student_profiles_batch FOREIGN KEY (batch_id) REFERENCES batches (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS study_materials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(80) NOT NULL,
    batch_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_study_materials_stored_filename UNIQUE (stored_filename),
    INDEX idx_materials_batch (batch_id),
    CONSTRAINT fk_study_materials_batch FOREIGN KEY (batch_id) REFERENCES batches (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS academy_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    details VARCHAR(3000) NOT NULL,
    batch_id BIGINT,
    student_id BIGINT,
    event_date DATE,
    status VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    attachment_id BIGINT,
    PRIMARY KEY (id),
    INDEX idx_records_batch_type (batch_id, type),
    INDEX idx_records_student_type (student_id, type),
    CONSTRAINT fk_records_batch FOREIGN KEY (batch_id) REFERENCES batches (id),
    CONSTRAINT fk_records_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_records_attachment FOREIGN KEY (attachment_id) REFERENCES study_materials (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(100) NOT NULL,
    series VARCHAR(64) NOT NULL,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL,
    PRIMARY KEY (series),
    INDEX idx_persistent_logins_username (username)
) ENGINE=InnoDB;
