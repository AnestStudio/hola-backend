DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS auth_refresh_tokens;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS user_details;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(30) CHARACTER SET utf8mb4 NOT NULL UNIQUE,
    description VARCHAR(255) CHARACTER SET utf8mb4 NULL
);

INSERT INTO roles (role_name, description) VALUES
    ('USER', 'Người dùng thông thường (Sinh viên)'),
    ('ADMIN', 'Quản trị viên hệ thống'),
    ('LANDLORD', 'Chủ nhà trọ / Chủ bất động sản'),
    ('EMPLOYER', 'Nhà tuyển dụng việc làm');

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(150) CHARACTER SET utf8mb4 UNIQUE NOT NULL,
    password_hash VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL,
    full_name     VARCHAR(150) CHARACTER SET utf8mb4 NOT NULL,
    status        VARCHAR(30) CHARACTER SET utf8mb4 DEFAULT 'ACTIVE' NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by    VARCHAR(100) CHARACTER SET utf8mb4 NULL,
    updated_by    VARCHAR(100) CHARACTER SET utf8mb4 NULL
);

CREATE TABLE user_details (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNIQUE NOT NULL,
    mobile     VARCHAR(20) CHARACTER SET utf8mb4 NULL,
    gender     VARCHAR(10) CHARACTER SET utf8mb4 NULL,
    bod        DATE NULL,
    address    VARCHAR(500) CHARACTER SET utf8mb4 NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) CHARACTER SET utf8mb4 NULL,
    updated_by VARCHAR(100) CHARACTER SET utf8mb4 NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE auth_refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token_hash  VARCHAR(64) CHARACTER SET utf8mb4 NOT NULL UNIQUE,
    device_info VARCHAR(255) CHARACTER SET utf8mb4 NULL,
    ip_address  VARCHAR(45) CHARACTER SET utf8mb4 NULL,
    expires_at  DATETIME NOT NULL,
    revoked     TINYINT(1) DEFAULT 0 NOT NULL,
    revoked_at  DATETIME NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) CHARACTER SET utf8mb4 NULL,
    updated_by  VARCHAR(100) CHARACTER SET utf8mb4 NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    token_hash VARCHAR(64) CHARACTER SET utf8mb4 NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at    DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) CHARACTER SET utf8mb4 NULL,
    updated_by VARCHAR(100) CHARACTER SET utf8mb4 NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
