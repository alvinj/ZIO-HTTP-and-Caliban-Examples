
-- TEMPORARY: to get things working initially
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at INT NOT NULL DEFAULT 100
);

-- WHAT I REALLY WANT:
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at BIGINT NOT NULL DEFAULT 100
);
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

insert into users (name, email) values ('alvin', 'al@example.com');
insert into users (name, email) values ('betty', 'betty@example.com');
