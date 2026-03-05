-- ============================================================
-- Seed: userdb
-- 9 users: 1 admin, 5 freelancers, 3 clients
-- Password for all: "password123" (bcrypt)
-- Run AFTER starting the user microservice (so the table is created).
-- ============================================================
USE userdb;

INSERT IGNORE INTO users (id, email, password_hash, first_name, last_name, role, phone, avatar_url, is_active, created_at, updated_at) VALUES
(1, 'admin@smartfreelance.com',     '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Admin',  'User',     'ADMIN',       '+1-555-000-0001', NULL, 1, NOW(), NOW()),
(2, 'sarah.chen@email.com',         '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Sarah',  'Chen',     'FREELANCER',  '+1-555-100-0002', NULL, 1, NOW(), NOW()),
(3, 'marco.rivera@email.com',       '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Marco',  'Rivera',   'FREELANCER',  '+1-555-100-0003', NULL, 1, NOW(), NOW()),
(4, 'aisha.patel@email.com',        '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Aisha',  'Patel',    'FREELANCER',  '+1-555-100-0004', NULL, 1, NOW(), NOW()),
(5, 'james.kim@email.com',          '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'James',  'Kim',      'FREELANCER',  '+1-555-100-0005', NULL, 1, NOW(), NOW()),
(6, 'elena.vasquez@email.com',      '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Elena',  'Vasquez',  'FREELANCER',  '+1-555-100-0006', NULL, 1, NOW(), NOW()),
(7, 'alex.johnson@email.com',       '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Alex',   'Johnson',  'CLIENT',      '+1-555-200-0007', NULL, 1, NOW(), NOW()),
(8, 'maria.garcia@email.com',       '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Maria',  'Garcia',   'CLIENT',      '+1-555-200-0008', NULL, 1, NOW(), NOW()),
(9, 'david.wilson@email.com',       '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'David',  'Wilson',   'CLIENT',      '+1-555-200-0009', NULL, 1, NOW(), NOW());
