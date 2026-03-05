-- ============================================================
-- Seed: projectdb
-- Projects posted by clients + freelancer applications
-- Run AFTER starting the Project microservice.
-- Note: Table name is "project" (no @Table annotation on Project class).
-- ============================================================
USE projectdb;

-- ── Projects ────────────────────────────────────────────────
INSERT IGNORE INTO project
  (id, client_id, title, description, budget, deadline, status, category, created_at, updated_at)
VALUES
-- Alex Johnson (7) projects
(1, 7, 'B2B E-Commerce Platform',
   'Build a full-featured B2B e-commerce platform with Angular frontend, Spring Boot backend, and MySQL database. Features: product catalogue, bulk ordering, invoicing, and role-based access control.',
   15000.00, DATE_ADD(NOW(), INTERVAL 4 MONTH), 'IN_PROGRESS', 'Web Development',
   DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(4, 7, 'SaaS Dashboard UI/UX Redesign',
   'Complete visual and UX overhaul of our B2B SaaS analytics dashboard. Deliverables: Figma designs, component library, and implementation guidance. Current design is outdated and confusing.',
   5500.00, DATE_ADD(NOW(), INTERVAL 2 MONTH), 'OPEN', 'UI/UX Design',
   DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- Maria Garcia (8) projects
(2, 8, 'Mobile Health App Backend APIs',
   'Develop RESTful APIs for a mobile health monitoring application. Requirements: JWT auth, patient/doctor roles, appointment scheduling, HL7-compliant data export, and MySQL persistence.',
   12000.00, DATE_ADD(NOW(), INTERVAL 3 MONTH), 'IN_PROGRESS', 'Backend Development',
   DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(5, 8, 'Patient Mobile App (iOS & Android)',
   'Cross-platform mobile app for patients to track vitals, view appointments, and chat with doctors. React Native preferred. BLE wearable integration required. Must pass HIPAA compliance.',
   18000.00, DATE_ADD(NOW(), INTERVAL 4 MONTH), 'IN_PROGRESS', 'Mobile Development',
   DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),

-- David Wilson (9) projects
(3, 9, 'AWS DevOps Pipeline & Migration',
   'Migrate our monolithic Java application to AWS ECS with full CI/CD automation using GitHub Actions and Terraform. Target: zero-downtime deployments, 99.9% SLA, and 30% cost reduction.',
   20000.00, DATE_SUB(NOW(), INTERVAL 1 MONTH), 'COMPLETED', 'DevOps / Cloud',
   DATE_SUB(NOW(), INTERVAL 5 MONTH), DATE_SUB(NOW(), INTERVAL 1 MONTH));

-- ── Project → required skill IDs ────────────────────────────
-- Links each project to relevant Portfolio skill IDs
INSERT IGNORE INTO project_skill_ids (project_id, skill_id) VALUES
(1, 1),(1, 2),(1, 6),(1, 7),  -- E-Commerce: Angular, TypeScript, Spring Boot, MySQL
(2, 6),(2, 7),(2, 5),          -- Health APIs: Spring Boot, MySQL, Java
(3, 13),(3, 14),(3, 15),(3,16),-- DevOps: Docker, Kubernetes, AWS, CI/CD
(4, 9),(4, 10),(4, 11),        -- UX Redesign: Figma, Adobe XD, UX Research
(5, 17),(5, 18),(5, 19);       -- Mobile: React Native, Flutter, Kotlin

-- ── Project Applications ────────────────────────────────────
INSERT IGNORE INTO project_application
  (id, freelance_id, cover_letter, proposed_price, proposed_duration, status, applied_at, project_id)
VALUES
-- Sarah (2) applies to E-Commerce (1) → ACCEPTED
(1, 2,
   'I have 3 years of professional Angular experience and have delivered two similar B2B platforms. My last project (ShopStart) handles 50k SKUs and 500 daily orders. I can start immediately.',
   14500.00, 120, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 22 DAY), 1),

-- Marco (3) applies to Health APIs (2) → ACCEPTED
(2, 3,
   'Spring Boot microservices is my core expertise. I have built HIPAA-compliant APIs for two healthcare clients and can deliver all your requirements including HL7 export within 3 months.',
   11500.00, 90, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 12 DAY), 2),

-- James (5) applies to DevOps (3) → ACCEPTED (project is COMPLETED)
(3, 5,
   'I have executed 3 monolith-to-AWS migrations. My last project reduced infrastructure costs by 35% and achieved 99.95% uptime. I have ready-made Terraform modules for ECS. Timeline: 4 months.',
   19000.00, 120, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 5 MONTH), 3),

-- Aisha (4) applies to UI Redesign (4) → PENDING
(4, 4,
   'Dashboard redesigns are my specialty. I have improved user task completion rates by 40% on similar SaaS products through rigorous UX research and iterative prototyping in Figma.',
   5000.00, 60, 'PENDING', DATE_SUB(NOW(), INTERVAL 3 DAY), 4),

-- Elena (6) applies to Mobile App (5) → ACCEPTED
(5, 6,
   'I built FitLife — a React Native health tracker with BLE wearable integration — that hit 4.8★ on the App Store. HIPAA-compliant architecture is something I have implemented before. Perfect fit!',
   17500.00, 120, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 10 DAY), 5),

-- Marco (3) also applies to E-Commerce backend (1) → PENDING
(6, 3,
   'I can handle the Spring Boot backend portion of your e-commerce project, including REST APIs, Hibernate data layer, and JWT security. Available immediately for a 3-month backend engagement.',
   8000.00, 90, 'PENDING', DATE_SUB(NOW(), INTERVAL 18 DAY), 1);
