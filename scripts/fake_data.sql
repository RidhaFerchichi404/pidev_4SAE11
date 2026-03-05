-- ==========================================
-- FAKE DATA INJECTION SCRIPT (FULLY QUALIFIED)
-- ==========================================
-- This script uses fully qualified table names (database.table) to avoid "Table doesn't exist" errors.
-- It works regardless of which database is currently selected in phpMyAdmin.
-- Password for all users: test

-- ==========================================
-- 1. USERS (userdb)
-- ==========================================
INSERT IGNORE INTO userdb.users (id, email, password_hash, first_name, last_name, role) VALUES 
(1, 'freelancer1@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Alice', 'Dev', 'FREELANCER'),
(2, 'freelancer2@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Bob', 'Coder', 'FREELANCER'),
(3, 'freelancer3@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Charlie', 'Java', 'FREELANCER'),
(4, 'freelancer4@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'David', 'Spring', 'FREELANCER'),
(5, 'freelancer5@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Eve', 'Frontend', 'FREELANCER'),
(6, 'freelancer6@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Frank', 'Backend', 'FREELANCER'),
(7, 'freelancer7@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Grace', 'Fullstack', 'FREELANCER'),
(8, 'freelancer8@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Hank', 'DevOps', 'FREELANCER'),
(9, 'freelancer9@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Ivy', 'Mobile', 'FREELANCER'),
(10, 'freelancer10@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Jack', 'Data', 'FREELANCER'),
(11, 'freelancer11@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Kate', 'Design', 'FREELANCER'),
(12, 'freelancer12@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Leo', 'Cloud', 'FREELANCER'),
(13, 'freelancer13@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Mia', 'Security', 'FREELANCER'),
(14, 'freelancer14@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Noah', 'Network', 'FREELANCER'),
(15, 'freelancer15@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Olivia', 'AI', 'FREELANCER'),
(16, 'freelancer16@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Paul', 'ML', 'FREELANCER'),
(17, 'freelancer17@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Quinn', 'Game', 'FREELANCER'),
(18, 'freelancer18@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Ryan', 'Web', 'FREELANCER'),
(19, 'freelancer19@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Sara', 'App', 'FREELANCER'),
(20, 'freelancer20@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Tom', 'Tech', 'FREELANCER');

INSERT IGNORE INTO userdb.users (id, email, password_hash, first_name, last_name, role) VALUES 
(21, 'client1@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Alpha', 'Corp', 'CLIENT'),
(22, 'client2@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Beta', 'Inc', 'CLIENT'),
(23, 'client3@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Gamma', 'Ltd', 'CLIENT'),
(24, 'client4@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Delta', 'Group', 'CLIENT'),
(25, 'client5@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Epsilon', 'Systems', 'CLIENT'),
(26, 'client6@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Zeta', 'Solutions', 'CLIENT'),
(27, 'client7@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Eta', 'Enterprises', 'CLIENT'),
(28, 'client8@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Theta', 'Tech', 'CLIENT'),
(29, 'client9@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Iota', 'Industries', 'CLIENT'),
(30, 'client10@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Kappa', 'Holdings', 'CLIENT'),
(31, 'client11@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Lambda', 'LLC', 'CLIENT'),
(32, 'client12@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Mu', 'Global', 'CLIENT'),
(33, 'client13@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Nu', 'Networks', 'CLIENT'),
(34, 'client14@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Xi', 'Ventures', 'CLIENT'),
(35, 'client15@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Omicron', 'Partners', 'CLIENT'),
(36, 'client16@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Pi', 'Productions', 'CLIENT'),
(37, 'client17@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Rho', 'Records', 'CLIENT'),
(38, 'client18@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Sigma', 'Software', 'CLIENT'),
(39, 'client19@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Tau', 'Technologies', 'CLIENT'),
(40, 'client20@test.com', '$2a$10$fM4Wtfoeu7M7S6U1SZESZe9oDBk538ZFs5S9yNpvWFrsX8VjriQRi', 'Upsilon', 'Media', 'CLIENT');


-- ==========================================
-- 2. PORTFOLIO (portfolio_db)
-- ==========================================
INSERT IGNORE INTO portfolio_db.skills (id, name, user_id, created_at, updated_at) VALUES 
(1, 'Java', 1, NOW(), NOW()), (2, 'Spring Boot', 1, NOW(), NOW()), (3, 'Microservices', 1, NOW(), NOW()),
(4, 'React', 2, NOW(), NOW()), (5, 'JavaScript', 2, NOW(), NOW()), (6, 'HTML/CSS', 2, NOW(), NOW()),
(7, 'Python', 3, NOW(), NOW()), (8, 'Django', 3, NOW(), NOW()), (9, 'Flask', 3, NOW(), NOW()),
(10, 'Node.js', 4, NOW(), NOW()), (11, 'Express', 4, NOW(), NOW()), (12, 'MongoDB', 4, NOW(), NOW()),
(13, 'Docker', 5, NOW(), NOW()), (14, 'Kubernetes', 5, NOW(), NOW()), (15, 'Jenkins', 5, NOW(), NOW()),
(16, 'AWS', 6, NOW(), NOW()), (17, 'Azure', 6, NOW(), NOW()), (18, 'GCP', 6, NOW(), NOW()),
(19, 'C#', 7, NOW(), NOW()), (20, '.NET Core', 7, NOW(), NOW()), (21, 'SQL Server', 7, NOW(), NOW()),
(22, 'PHP', 8, NOW(), NOW()), (23, 'Laravel', 8, NOW(), NOW()), (24, 'MySQL', 8, NOW(), NOW()),
(25, 'Swift', 9, NOW(), NOW()), (26, 'iOS', 9, NOW(), NOW()), (27, 'Xcode', 9, NOW(), NOW()),
(28, 'Kotlin', 10, NOW(), NOW()), (29, 'Android', 10, NOW(), NOW()), (30, 'Jetpack Compose', 10, NOW(), NOW()),
(31, 'Figma', 11, NOW(), NOW()), (32, 'UI Design', 11, NOW(), NOW()), (33, 'UX Research', 11, NOW(), NOW()),
(34, 'Terraform', 12, NOW(), NOW()), (35, 'Ansible', 12, NOW(), NOW()), (36, 'Linux', 12, NOW(), NOW()),
(37, 'Cybersecurity', 13, NOW(), NOW()), (38, 'Penetration Testing', 13, NOW(), NOW()), (39, 'Network Security', 13, NOW(), NOW()),
(40, 'Cisco', 14, NOW(), NOW()), (41, 'Routing', 14, NOW(), NOW()), (42, 'Switching', 14, NOW(), NOW()),
(43, 'Machine Learning', 15, NOW(), NOW()), (44, 'TensorFlow', 15, NOW(), NOW()), (45, 'PyTorch', 15, NOW(), NOW()),
(46, 'Data Analysis', 16, NOW(), NOW()), (47, 'Pandas', 16, NOW(), NOW()), (48, 'Tableau', 16, NOW(), NOW()),
(49, 'Unity', 17, NOW(), NOW()), (50, 'C++', 17, NOW(), NOW()), (51, '3D Modeling', 17, NOW(), NOW()),
(52, 'Vue.js', 18, NOW(), NOW()), (53, 'TypeScript', 18, NOW(), NOW()), (54, 'SASS', 18, NOW(), NOW()),
(55, 'Flutter', 19, NOW(), NOW()), (56, 'Dart', 19, NOW(), NOW()), (57, 'Firebase', 19, NOW(), NOW()),
(58, 'Technical Writing', 20, NOW(), NOW()), (59, 'Documentation', 20, NOW(), NOW()), (60, 'Markdown', 20, NOW(), NOW());

INSERT IGNORE INTO portfolio_db.skill_domains (skill_id, domains) VALUES 
(1, 'BACKEND'), (2, 'BACKEND'), (3, 'BACKEND'),
(4, 'FRONTEND'), (5, 'FRONTEND'), (6, 'FRONTEND'),
(7, 'BACKEND'), (8, 'BACKEND'), (9, 'BACKEND'),
(10, 'BACKEND'), (11, 'BACKEND'), (12, 'BACKEND'),
(13, 'DEVOPS'), (14, 'DEVOPS'), (15, 'DEVOPS'),
(16, 'DEVOPS'), (17, 'DEVOPS'), (18, 'DEVOPS'),
(19, 'BACKEND'), (20, 'BACKEND'), (21, 'BACKEND'),
(22, 'BACKEND'), (23, 'BACKEND'), (24, 'BACKEND'),
(25, 'MOBILE'), (26, 'MOBILE'), (27, 'MOBILE'),
(28, 'MOBILE'), (29, 'MOBILE'), (30, 'MOBILE'),
(31, 'DESIGN'), (32, 'DESIGN'), (33, 'DESIGN'),
(34, 'DEVOPS'), (35, 'DEVOPS'), (36, 'DEVOPS'),
(37, 'SECURITY'), (38, 'SECURITY'), (39, 'SECURITY'),
(40, 'NETWORKING'), (41, 'NETWORKING'), (42, 'NETWORKING'),
(43, 'AI_ML'), (44, 'AI_ML'), (45, 'AI_ML'),
(46, 'DATA'), (47, 'DATA'), (48, 'DATA'),
(49, 'GAME_DEV'), (50, 'GAME_DEV'), (51, 'GAME_DEV'),
(52, 'FRONTEND'), (53, 'FRONTEND'), (54, 'FRONTEND'),
(55, 'MOBILE'), (56, 'MOBILE'), (57, 'MOBILE'),
(58, 'WRITING'), (59, 'WRITING'), (60, 'WRITING');

INSERT IGNORE INTO portfolio_db.experiences (id, user_id, title, type, domain, company_or_client_name, start_date, end_date, description) VALUES
(1, 1, 'Senior Java Developer', 'JOB', 'BACKEND', 'Tech Corp', '2020-01-01', '2022-01-01', 'Developed microservices.'),
(2, 1, 'Freelance Backend Dev', 'PROJECT', 'BACKEND', 'Startup X', '2022-02-01', NULL, 'Building API gateway.'),
(3, 2, 'Frontend Engineer', 'JOB', 'FRONTEND', 'Web Solutions', '2021-06-01', '2023-01-01', 'React development.'),
(4, 5, 'DevOps Specialist', 'JOB', 'DEVOPS', 'Cloud Systems', '2019-01-01', '2021-01-01', 'CI/CD pipelines.');


-- ==========================================
-- 3. PROJECTS (projectdb)
-- ==========================================
INSERT IGNORE INTO projectdb.project (id, client_id, title, description, budget, status, category, created_at, updated_at) VALUES 
(1, 21, 'E-commerce Platform Phase 1', 'Initial setup.', 5000.00, 'COMPLETED', 'WEB_DEVELOPMENT', NOW(), NOW()),
(2, 21, 'E-commerce Platform Phase 2', 'Payment gateway.', 3000.00, 'COMPLETED', 'WEB_DEVELOPMENT', NOW(), NOW()),
(3, 21, 'E-commerce Mobile App', 'iOS App.', 8000.00, 'IN_PROGRESS', 'MOBILE_DEVELOPMENT', NOW(), NOW()),
(4, 21, 'E-commerce SEO', 'Marketing push.', 1000.00, 'OPEN', 'MARKETING', NOW(), NOW()),
(5, 21, 'Customer Support Bot', 'AI integration.', 2000.00, 'OPEN', 'AI_ML', NOW(), NOW()),
(6, 21, 'Legacy System Migration', 'Move to cloud.', 15000.00, 'COMPLETED', 'DEVOPS', NOW(), NOW()),
(7, 21, 'Internal Dashboard', 'Admin panel.', 4000.00, 'COMPLETED', 'WEB_DEVELOPMENT', NOW(), NOW()),
(8, 21, 'Security Audit 2024', 'Annual check.', 2500.00, 'COMPLETED', 'CYBERSECURITY', NOW(), NOW()),
(9, 21, 'Blog Content', 'Technical articles.', 500.00, 'OPEN', 'WRITING', NOW(), NOW()),
(10, 21, 'Logo Rebrand', 'New identity.', 1000.00, 'COMPLETED', 'GRAPHIC_DESIGN', NOW(), NOW()),
(11, 22, 'Delivery App iOS', 'Native app.', 12000.00, 'COMPLETED', 'MOBILE_DEVELOPMENT', NOW(), NOW()),
(12, 22, 'Delivery App Android', 'Native app.', 12000.00, 'COMPLETED', 'MOBILE_DEVELOPMENT', NOW(), NOW()),
(13, 22, 'Backend API', 'Node.js API.', 8000.00, 'COMPLETED', 'BACKEND', NOW(), NOW()),
(14, 22, 'Driver App', 'For logistics.', 5000.00, 'IN_PROGRESS', 'MOBILE_DEVELOPMENT', NOW(), NOW()),
(15, 22, 'Admin Portal', 'Web based.', 4000.00, 'OPEN', 'WEB_DEVELOPMENT', NOW(), NOW()),
(16, 22, 'Marketing Video', 'Promo.', 1500.00, 'OPEN', 'VIDEO_EDITING', NOW(), NOW()),
(17, 22, 'Social Media Manager', 'Monthly.', 800.00, 'IN_PROGRESS', 'MARKETING', NOW(), NOW()),
(18, 22, 'Data Analysis', 'User retention.', 3000.00, 'COMPLETED', 'DATA_SCIENCE', NOW(), NOW()),
(19, 22, 'Bug Fixes', 'Maintenance.', 500.00, 'COMPLETED', 'BACKEND', NOW(), NOW()),
(20, 22, 'Feature: Chat', 'In-app chat.', 2000.00, 'OPEN', 'MOBILE_DEVELOPMENT', NOW(), NOW()),
(21, 23, 'Corporate Site v2', 'React update.', 5000.00, 'COMPLETED', 'WEB_DEVELOPMENT', NOW(), NOW()),
(22, 23, 'CMS Integration', 'Strapi setup.', 2000.00, 'COMPLETED', 'BACKEND', NOW(), NOW()),
(23, 23, 'Employee Portal', 'Intranet.', 6000.00, 'IN_PROGRESS', 'WEB_DEVELOPMENT', NOW(), NOW()),
(24, 23, 'Training Videos', 'Editing.', 1000.00, 'OPEN', 'VIDEO_EDITING', NOW(), NOW()),
(25, 23, 'Newsletter Setup', 'Mailchimp.', 300.00, 'COMPLETED', 'MARKETING', NOW(), NOW()),
(26, 23, 'SEO Audit', 'Technical SEO.', 800.00, 'COMPLETED', 'MARKETING', NOW(), NOW()),
(27, 23, 'Cloud Backup', 'AWS S3.', 1500.00, 'OPEN', 'DEVOPS', NOW(), NOW()),
(28, 23, 'CRM Setup', 'Salesforce.', 5000.00, 'OPEN', 'ADMIN', NOW(), NOW()),
(29, 23, 'Mobile Site', 'Responsive fix.', 1000.00, 'COMPLETED', 'WEB_DEVELOPMENT', NOW(), NOW()),
(30, 23, 'Performance Tuning', 'Faster load times.', 2000.00, 'IN_PROGRESS', 'DEVOPS', NOW(), NOW());

-- Project Skills
INSERT IGNORE INTO projectdb.project_skill_ids (project_id, skill_id) VALUES 
(1, 1), (1, 2), (1, 3), 
(2, 9), (2, 4), 
(4, 5), (4, 11),
(9, 11), (9, 10);


-- ==========================================
-- 4. CONTRACTS (gestion_contract_db)
-- ==========================================
INSERT IGNORE INTO gestion_contract_db.contracts (id, client_id, freelancer_id, project_application_id, title, description, amount, status, start_date, end_date, created_at) VALUES
(1, 23, 1, 101, 'Website Redesign Contract', 'Contract for full redesign.', 2000.00, 'ACTIVE', '2023-01-01', '2023-03-01', NOW()),
(2, 28, 5, 102, 'Cloud Migration Contract', 'Migration to AWS.', 10000.00, 'ACTIVE', '2023-02-01', '2023-08-01', NOW()),
(3, 24, 10, 103, 'Data Dashboard Contract', 'Building Tableau dashboard.', 3000.00, 'COMPLETED', '2022-01-01', '2022-02-01', NOW());


-- ==========================================
-- 5. OFFERS (gestion_offre_db)
-- ==========================================
INSERT IGNORE INTO gestion_offre_db.offers (id, freelancer_id, title, domain, description, price, duration_type, offer_status) VALUES
(1, 1, 'Java Backend Development', 'BACKEND', 'I will build robust Java APIs.', 50.00, 'hourly', 'OPEN'),
(2, 2, 'React Frontend Development', 'FRONTEND', 'Responsive UI with React.', 40.00, 'hourly', 'OPEN'),
(3, 3, 'Python Scripting', 'BACKEND', 'Automation scripts.', 30.00, 'hourly', 'OPEN'),
(4, 5, 'DevOps Setup', 'DEVOPS', 'CI/CD pipeline setup.', 500.00, 'fixed', 'OPEN'),
(5, 7, 'Fullstack Web App', 'FULLSTACK', 'Complete web application.', 2000.00, 'fixed', 'OPEN');


-- ==========================================
-- 6. PLANNING (planningdb)
-- ==========================================
INSERT IGNORE INTO planningdb.progress_update (id, project_id, freelancer_id, title, description, progress_percentage, created_at, updated_at) VALUES
(1, 3, 1, 'Initial Design', 'Completed wireframes.', 20, NOW(), NOW()),
(2, 3, 1, 'Frontend Implementation', 'Started React components.', 40, NOW(), NOW()),
(3, 9, 5, 'Infrastructure Audit', 'Analyzed current servers.', 10, NOW(), NOW()),
(4, 9, 5, 'AWS Setup', 'Created VPC and EC2 instances.', 30, NOW(), NOW());
