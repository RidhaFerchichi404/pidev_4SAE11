-- ============================================================
-- Seed: taskdb
-- Tasks and task comments for active/completed projects
-- Run AFTER starting the task microservice.
-- ============================================================
USE taskdb;

-- ── Tasks ────────────────────────────────────────────────────
-- project_id references projects in projectdb (cross-service by ID convention)
-- assignee_id = freelancer working on the task
-- created_by  = client or freelancer who created the task
-- status: TODO | IN_PROGRESS | IN_REVIEW | DONE
-- priority: LOW | MEDIUM | HIGH | URGENT
INSERT IGNORE INTO task
  (id, project_id, contract_id, title, description, status, priority,
   assignee_id, due_date, order_index, parent_task_id, created_by, created_at, updated_at)
VALUES
-- ── E-Commerce project (project 1) — Sarah (2) — contract 1 ──
(1,  1, 1, 'Project Setup & Architecture',
   'Initialise Angular workspace, configure NgRx store, set up routing and lazy loading, integrate Bootstrap/Tailwind theme.',
   'DONE', 'HIGH', 2, CURDATE() - INTERVAL 15 DAY, 1, NULL, 7, DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),

(2,  1, 1, 'Product Catalogue Module',
   'Implement product listing, search/filter, pagination, and product detail page with image gallery.',
   'IN_REVIEW', 'HIGH', 2, CURDATE() + INTERVAL 5 DAY, 2, NULL, 7, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(3,  1, 1, 'Shopping Cart & Checkout Flow',
   'Build shopping cart with NgRx, multi-step checkout, address book, and Stripe payment integration.',
   'IN_PROGRESS', 'HIGH', 2, CURDATE() + INTERVAL 20 DAY, 3, NULL, 7, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(4,  1, 1, 'User Account & Order History',
   'User profile, past orders, re-order functionality, and downloadable invoice PDFs.',
   'TODO', 'MEDIUM', 2, CURDATE() + INTERVAL 35 DAY, 4, NULL, 7, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

(5,  1, 1, 'Admin Panel',
   'Backoffice panel for managing products, orders, and users. Role-based access control.',
   'TODO', 'MEDIUM', 2, CURDATE() + INTERVAL 50 DAY, 5, NULL, 7, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- ── Mobile Health App (project 5) — Elena (6) ──
(6,  5, 3, 'React Native Project Bootstrap',
   'Set up Expo project, navigation library, state management with Zustand, and CI/CD on EAS.',
   'DONE', 'HIGH', 6, CURDATE() - INTERVAL 5 DAY, 1, NULL, 8, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

(7,  5, 3, 'Authentication Screens',
   'Login, register, forgot password, and biometric unlock using React Native Keychain.',
   'IN_PROGRESS', 'HIGH', 6, CURDATE() + INTERVAL 10 DAY, 2, NULL, 8, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

(8,  5, 3, 'BLE Wearable Integration',
   'Connect to Bluetooth Low Energy heart rate and SpO2 sensors; store readings in local DB.',
   'TODO', 'URGENT', 6, CURDATE() + INTERVAL 30 DAY, 3, NULL, 8, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

-- ── DevOps Project (project 3) — James (5) — COMPLETED ──
(9,  3, 2, 'Infrastructure as Code with Terraform',
   'Write Terraform modules for VPC, ECS cluster, RDS, ALB, and S3 buckets.',
   'DONE', 'HIGH', 5, CURDATE() - INTERVAL 4 MONTH, 1, NULL, 9, DATE_SUB(NOW(), INTERVAL 5 MONTH), DATE_SUB(NOW(), INTERVAL 4 MONTH)),

(10, 3, 2, 'Dockerise Java Application',
   'Write multi-stage Dockerfile, push to ECR, configure environment-specific profiles.',
   'DONE', 'HIGH', 5, CURDATE() - INTERVAL 3 MONTH - INTERVAL 15 DAY, 2, NULL, 9, DATE_SUB(NOW(), INTERVAL 5 MONTH), DATE_SUB(NOW(), INTERVAL 3 MONTH - INTERVAL 15 DAY)),

(11, 3, 2, 'GitHub Actions CI/CD Pipeline',
   'Build → Test → Docker build → Push to ECR → Deploy to ECS pipeline with staging and prod environments.',
   'DONE', 'HIGH', 5, CURDATE() - INTERVAL 2 MONTH, 3, NULL, 9, DATE_SUB(NOW(), INTERVAL 4 MONTH), DATE_SUB(NOW(), INTERVAL 2 MONTH)),

(12, 3, 2, 'Production Go-Live & Monitoring',
   'Blue/green deployment, CloudWatch dashboards, PagerDuty alerts, and runbook documentation.',
   'DONE', 'URGENT', 5, CURDATE() - INTERVAL 1 MONTH, 4, NULL, 9, DATE_SUB(NOW(), INTERVAL 2 MONTH), DATE_SUB(NOW(), INTERVAL 1 MONTH));

-- ── Task Comments ─────────────────────────────────────────────
INSERT IGNORE INTO task_comment
  (id, task_id, user_id, message, created_at)
VALUES
-- Comments on E-Commerce tasks
(1, 2, 7,
   'Sarah, the product listing looks great! One request: can we add a "Compare" button for up to 4 products on the listing page?',
   DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, 2,
   'Absolutely! I will add the comparison feature in today''s build. It fits nicely into the existing filter panel. Will demo it in tomorrow''s standup.',
   DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 3, 2,
   'Stripe integration is working in test mode. Moving to Stripe Checkout for PCI compliance — slightly different flow but much safer. Flagging for your awareness.',
   DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- Comments on DevOps tasks
(4, 9, 9,
   'James, can we also add a separate Terraform workspace for staging? We want isolated environments.',
   DATE_SUB(NOW(), INTERVAL 4 MONTH + INTERVAL 5 DAY)),
(5, 9, 5,
   'Already in progress! I use Terraform workspaces with a shared module and separate var files per env. Will show you the structure in the next call.',
   DATE_SUB(NOW(), INTERVAL 4 MONTH + INTERVAL 4 DAY));
