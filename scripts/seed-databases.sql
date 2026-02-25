-- =============================================================================
-- Smart Freelance - Seed script: 10 rows per table in each database
-- Run: mysql -u root -p < scripts/seed-databases.sql
-- (omit -p if your MySQL root has no password)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. USER DB (userdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS userdb;
USE userdb;

INSERT IGNORE INTO users (email, password_hash, first_name, last_name, role, phone, avatar_url, is_active, created_at, updated_at) VALUES
('alice.client@example.com', '$2a$10$dummy.hash.alice', 'Alice', 'Martin', 'CLIENT', '+33612345678', NULL, 1, NOW(), NOW()),
('bob.freelance@example.com', '$2a$10$dummy.hash.bob', 'Bob', 'Dupont', 'FREELANCER', '+33623456789', NULL, 1, NOW(), NOW()),
('carol.client@example.com', '$2a$10$dummy.hash.carol', 'Carol', 'Bernard', 'CLIENT', '+33634567890', NULL, 1, NOW(), NOW()),
('david.freelance@example.com', '$2a$10$dummy.hash.david', 'David', 'Petit', 'FREELANCER', '+33645678901', NULL, 1, NOW(), NOW()),
('eve.client@example.com', '$2a$10$dummy.hash.eve', 'Eve', 'Leroy', 'CLIENT', '+33656789012', NULL, 1, NOW(), NOW()),
('frank.freelance@example.com', '$2a$10$dummy.hash.frank', 'Frank', 'Moreau', 'FREELANCER', '+33667890123', NULL, 1, NOW(), NOW()),
('grace.freelance@example.com', '$2a$10$dummy.hash.grace', 'Grace', 'Simon', 'FREELANCER', '+33678901234', NULL, 1, NOW(), NOW()),
('henry.freelance@example.com', '$2a$10$dummy.hash.henry', 'Henry', 'Laurent', 'FREELANCER', '+33689012345', NULL, 1, NOW(), NOW()),
('iris.freelance@example.com', '$2a$10$dummy.hash.iris', 'Iris', 'Lefebvre', 'FREELANCER', '+33690123456', NULL, 1, NOW(), NOW()),
('admin@smartfreelance.com', '$2a$10$dummy.hash.admin', 'Admin', 'System', 'ADMIN', '+33000000000', NULL, 1, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 2. PORTFOLIO DB (portfolio_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS portfolio_db;
USE portfolio_db;

-- Skills (user_id 1-10)
INSERT INTO skills (name, domain, description, user_id, experience_id, created_at, updated_at) VALUES
('Java', 'Backend', 'Java and Spring Boot development', 1, NULL, NOW(), NOW()),
('Angular', 'Frontend', 'Angular and TypeScript', 2, NULL, NOW(), NOW()),
('Python', 'Data', 'Python and data analysis', 3, NULL, NOW(), NOW()),
('React', 'Frontend', 'React and Redux', 4, NULL, NOW(), NOW()),
('MySQL', 'Database', 'MySQL and SQL design', 5, NULL, NOW(), NOW()),
('Node.js', 'Backend', 'Node.js and Express', 6, NULL, NOW(), NOW()),
('UI/UX Design', 'Design', 'Figma and user research', 7, NULL, NOW(), NOW()),
('DevOps', 'Infrastructure', 'Docker and CI/CD', 8, NULL, NOW(), NOW()),
('Machine Learning', 'Data', 'ML models and scikit-learn', 9, NULL, NOW(), NOW()),
('Project Management', 'Management', 'Agile and Scrum', 10, NULL, NOW(), NOW());

-- Experiences (user_id 1-10)
INSERT INTO experiences (user_id, title, type, description, start_date, end_date, company_or_client_name) VALUES
(1, 'Backend Developer at TechCorp', 'JOB', 'Developed REST APIs with Spring Boot', '2020-01-15', '2022-06-30', 'TechCorp'),
(2, 'E-commerce Frontend', 'PROJECT', 'Angular SPA for online store', '2021-03-01', '2021-08-31', 'ShopClient'),
(3, 'Data Analyst Intern', 'JOB', 'Python pipelines and reporting', '2019-09-01', '2020-02-28', 'DataCo'),
(4, 'React Dashboard', 'PROJECT', 'Real-time dashboard for logistics', '2022-01-10', '2022-05-20', 'LogiSoft'),
(5, 'DBA Consultant', 'JOB', 'Database design and optimization', '2018-06-01', '2021-12-31', 'DBExperts'),
(6, 'API Gateway', 'PROJECT', 'Node.js microservices gateway', '2022-06-01', '2022-11-15', 'API Client'),
(7, 'Mobile App UI', 'PROJECT', 'Figma designs for fitness app', '2021-07-01', '2021-10-30', 'FitApp'),
(8, 'CI/CD Pipeline', 'PROJECT', 'Jenkins and Docker setup', '2020-11-01', '2021-02-28', 'DevOps Inc'),
(9, 'Recommendation Engine', 'PROJECT', 'ML model for e-commerce', '2022-03-01', '2022-07-31', 'RecoClient'),
(10, 'Scrum Master', 'JOB', 'Agile coaching for dev team', '2019-01-01', '2023-06-30', 'AgileCorp');

-- Evaluation tests (skill_id 1-10)
INSERT INTO evaluation_tests (skill_id, title, passing_score, duration_minutes, created_at, updated_at) VALUES
(1, 'Java Basics Quiz', 70.0, 30, NOW(), NOW()),
(2, 'Angular Components Test', 75.0, 45, NOW(), NOW()),
(3, 'Python Fundamentals', 65.0, 40, NOW(), NOW()),
(4, 'React Hooks Assessment', 70.0, 35, NOW(), NOW()),
(5, 'SQL Queries Test', 80.0, 45, NOW(), NOW()),
(6, 'Node.js API Test', 70.0, 50, NOW(), NOW()),
(7, 'UI Design Principles', 75.0, 40, NOW(), NOW()),
(8, 'Docker & CI/CD', 70.0, 45, NOW(), NOW()),
(9, 'ML Basics Assessment', 65.0, 60, NOW(), NOW()),
(10, 'Agile & Scrum Quiz', 70.0, 30, NOW(), NOW());

-- Evaluations (freelancer_id 2,4,6,7,8,9; skill_id 1-6 - one per skill for coherence)
INSERT INTO evaluations (freelancer_id, skill_id, score, passed, test_result, evaluated_at) VALUES
(2, 1, 85.0, 1, 'Passed with distinction', NOW()),
(2, 2, 90.0, 1, 'Excellent', NOW()),
(4, 3, 72.0, 1, 'Passed', NOW()),
(4, 4, 88.0, 1, 'Very good', NOW()),
(6, 5, 78.0, 1, 'Passed', NOW()),
(6, 6, 82.0, 1, 'Good', NOW()),
(7, 7, 91.0, 1, 'Outstanding', NOW()),
(8, 8, 75.0, 1, 'Passed', NOW()),
(9, 9, 80.0, 1, 'Passed', NOW()),
(9, 10, 70.0, 1, 'Passed', NOW());

-- -----------------------------------------------------------------------------
-- 3. PROJECT DB (projectdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS projectdb;
USE projectdb;

-- Projects (client_id: 1,3,5 = clients from users)
INSERT INTO project (client_id, title, description, budget, deadline, status, category, skills_requiered, created_at, updated_at) VALUES
(1, 'E-commerce Website', 'Full e-commerce platform with payment', 5000.00, DATE_ADD(NOW(), INTERVAL 60 DAY), 'OPEN', 'Web Development', 'Java,Angular,MySQL', NOW(), NOW()),
(1, 'Mobile App Backend', 'REST API for mobile application', 3000.00, DATE_ADD(NOW(), INTERVAL 45 DAY), 'OPEN', 'Backend', 'Java,Spring Boot', NOW(), NOW()),
(3, 'Data Dashboard', 'Analytics dashboard with charts', 2500.00, DATE_ADD(NOW(), INTERVAL 30 DAY), 'IN_PROGRESS', 'Data', 'React,Python', NOW(), NOW()),
(3, 'Logo Design', 'Brand identity and logo', 800.00, DATE_ADD(NOW(), INTERVAL 14 DAY), 'OPEN', 'Design', 'UI/UX', NOW(), NOW()),
(5, 'DevOps Setup', 'Docker and Kubernetes migration', 4000.00, DATE_ADD(NOW(), INTERVAL 90 DAY), 'OPEN', 'DevOps', 'Docker,K8s', NOW(), NOW()),
(5, 'API Integration', 'Third-party API integration', 1500.00, DATE_ADD(NOW(), INTERVAL 21 DAY), 'COMPLETED', 'Backend', 'Node.js', NOW(), NOW()),
(1, 'ML Recommendation', 'Product recommendation engine', 6000.00, DATE_ADD(NOW(), INTERVAL 120 DAY), 'OPEN', 'Machine Learning', 'Python,ML', NOW(), NOW()),
(3, 'Scrum Training', 'Agile training for team', 2000.00, DATE_ADD(NOW(), INTERVAL 20 DAY), 'OPEN', 'Training', 'Agile', NOW(), NOW()),
(5, 'Security Audit', 'Application security audit', 3500.00, DATE_ADD(NOW(), INTERVAL 40 DAY), 'OPEN', 'Security', 'Security,Java', NOW(), NOW()),
(1, 'Landing Page', 'Marketing landing page', 500.00, DATE_ADD(NOW(), INTERVAL 7 DAY), 'OPEN', 'Frontend', 'React,HTML', NOW(), NOW());

-- Project applications (freelance_id 2,4,6,7,8,9; project_id 1-10)
INSERT INTO project_application (freelance_id, cover_letter, proposed_price, proposed_duration, status, applied_at, responded_at, project_id) VALUES
(2, 'I have 3 years of Angular and e-commerce experience. I can deliver a modern SPA.', 4800.00, 55, 'PENDING', NOW(), NULL, 1),
(4, 'I built similar dashboards with React and D3. Ready to start.', 2400.00, 25, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 3),
(6, 'Node.js is my specialty. I have done many API integrations.', 1400.00, 18, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY), 6),
(7, 'I can create a distinctive logo and brand guidelines.', 750.00, 10, 'PENDING', NOW(), NULL, 4),
(8, 'I set up CI/CD for several companies. Docker and K8s ready.', 3800.00, 80, 'PENDING', NOW(), NULL, 5),
(9, 'I have implemented recommendation systems in production.', 5500.00, 100, 'PENDING', NOW(), NULL, 7),
(2, 'I can deliver a clean landing page with React in one week.', 450.00, 5, 'PENDING', NOW(), NULL, 10),
(4, 'Backend API with Spring Boot and documentation.', 2800.00, 40, 'REJECTED', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 2),
(6, 'Security-focused API design and implementation.', 3200.00, 35, 'PENDING', NOW(), NULL, 9),
(8, 'Agile workshops and Scrum certification prep.', 1800.00, 15, 'PENDING', NOW(), NULL, 8);

-- -----------------------------------------------------------------------------
-- 4. OFFER DB (gestion_offre_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_offre_db;
USE gestion_offre_db;

-- Offers (freelancer_id 2,4,6,7,8,9)
INSERT INTO offers (freelancer_id, title, domain, description, price, duration_type, offer_status, deadline, category, rating, communication_score, tags, views_count, is_featured, is_active, created_at, updated_at, published_at, expired_at) VALUES
(2, 'Angular Frontend Development', 'Frontend', 'Professional Angular development: components, state management, and testing. Clean code and documentation.', 50.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'Web', 4.80, 4.90, 'angular,typescript,spa', 120, 1, 1, NOW(), NOW(), NOW(), NULL),
(2, 'E-commerce SPA', 'Frontend', 'Full e-commerce single page application with cart and checkout.', 2500.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'E-commerce', 4.50, 4.70, 'angular,ecommerce', 85, 0, 1, NOW(), NOW(), NOW(), NULL),
(4, 'React Dashboard & Charts', 'Frontend', 'Custom React dashboards with Recharts or D3. Responsive and performant.', 45.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 120 DAY), 'Data Viz', 4.90, 4.85, 'react,d3,charts', 200, 1, 1, NOW(), NOW(), NOW(), NULL),
(6, 'Node.js REST API', 'Backend', 'RESTful API design and implementation with Node.js and Express. JWT auth included.', 55.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'Backend', 4.70, 4.80, 'node,express,api', 95, 0, 1, NOW(), NOW(), NOW(), NULL),
(7, 'UI/UX Design & Figma', 'Design', 'User research, wireframes, high-fidelity Figma designs, and design systems.', 60.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'Design', 4.95, 5.00, 'figma,ui,ux', 150, 1, 1, NOW(), NOW(), NOW(), NULL),
(8, 'Docker & CI/CD Setup', 'DevOps', 'Dockerize your app and set up Jenkins or GitHub Actions CI/CD.', 70.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'DevOps', 4.60, 4.75, 'docker,jenkins,ci', 70, 0, 1, NOW(), NOW(), NOW(), NULL),
(9, 'Machine Learning Model', 'Data', 'Custom ML models: classification, regression, or recommendation. Python and scikit-learn.', 80.00, 'hourly', 'DRAFT', DATE_ADD(CURDATE(), INTERVAL 180 DAY), 'ML', 4.85, 4.90, 'python,ml,sklearn', 40, 0, 1, NOW(), NOW(), NULL, NULL),
(9, 'Recommendation System', 'Data', 'Product or content recommendation engine. From design to deployment.', 5000.00, 'fixed', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'ML', 4.80, 4.85, 'recommendation,ml', 55, 0, 1, NOW(), NOW(), NOW(), NULL),
(10, 'Agile Coaching', 'Management', 'Scrum master services and Agile team coaching. Ceremonies and backlog refinement.', 90.00, 'hourly', 'AVAILABLE', DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'Agile', 4.70, 4.95, 'agile,scrum', 30, 0, 1, NOW(), NOW(), NOW(), NULL),
(4, 'Full Stack MVP', 'Full Stack', 'React + Node.js MVP in 4 weeks. From idea to deploy.', 3500.00, 'fixed', 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'MVP', 4.90, 4.95, 'react,node,mvp', 180, 1, 0, NOW(), NOW(), DATE_SUB(NOW(), INTERVAL 60 DAY), NULL);

-- Offer applications (offer_id 1-10, client_id 1,3,5)
INSERT INTO offer_applications (offer_id, client_id, message, proposed_budget, portfolio_url, attachment_url, estimated_duration, status, is_read, applied_at, responded_at, accepted_at) VALUES
(1, 1, 'I need an Angular developer for a 3-month project. Your profile matches. Can we discuss scope?', 12000.00, 'https://portfolio.example.com/alice', NULL, 90, 'PENDING', 0, NOW(), NULL, NULL),
(2, 3, 'We want to launch our e-commerce SPA by Q2. Your fixed price works for us.', 2500.00, NULL, 'https://storage.example.com/cv.pdf', 60, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), NOW()),
(3, 5, 'Dashboard for our internal analytics. Need charts and filters.', 3500.00, 'https://portfolio.example.com/carol', NULL, 45, 'PENDING', 0, NOW(), NULL, NULL),
(4, 1, 'We need a Node API for our mobile app. 6-month contract possible.', 8800.00, NULL, NULL, 80, 'SHORTLISTED', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
(5, 3, 'Redesign of our main product. UI/UX and Figma delivery.', 4200.00, 'https://design.example.com', NULL, 35, 'PENDING', 0, NOW(), NULL, NULL),
(6, 5, 'CI/CD for our Java microservices. Jenkins and Docker.', 5600.00, NULL, NULL, 40, 'REJECTED', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), NULL),
(7, 1, 'Exploring ML for our recommendation feature. Still in draft?', 9600.00, NULL, NULL, 60, 'PENDING', 0, NOW(), NULL, NULL),
(8, 3, 'We need a recommendation engine. Your fixed offer fits our budget.', 4800.00, NULL, 'https://storage.example.com/proposal.pdf', 70, 'PENDING', 0, NOW(), NULL, NULL),
(9, 5, 'Agile coaching for 2 teams. 3 months initial.', 5400.00, NULL, NULL, 60, 'PENDING', 0, NOW(), NULL, NULL),
(10, 1, 'We already worked together on the MVP. Great experience, applying again.', 3200.00, 'https://portfolio.example.com/alice', NULL, 25, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 58 DAY), DATE_SUB(NOW(), INTERVAL 58 DAY));

-- -----------------------------------------------------------------------------
-- 5. CONTRACT DB (gestion_contract_db)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS gestion_contract_db;
USE gestion_contract_db;

INSERT INTO contracts (client_id, freelancer_id, project_application_id, offer_application_id, title, description, terms, amount, start_date, end_date, status, signed_at, created_at) VALUES
(1, 2, 1, NULL, 'E-commerce Frontend Development', 'Angular development for e-commerce platform. Delivery of SPA with cart and checkout.', 'Standard NDA and IP assignment. Payment 50% upfront, 50% on delivery.', 4800.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 55 DAY), 'PENDING_SIGNATURE', NULL, NOW()),
(3, 4, 2, 2, 'Analytics Dashboard', 'React dashboard with charts and filters. Responsive and documented.', 'Milestone-based payments. 3 reviews included.', 2400.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'ACTIVE', NOW(), NOW()),
(5, 6, 3, NULL, 'API Integration', 'Node.js API integration for mobile backend. Documentation and tests.', 'Fixed price. One round of revisions.', 1400.00, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 12 DAY), 'COMPLETED', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(1, 7, NULL, 2, 'E-commerce SPA Delivery', 'Delivery of e-commerce SPA per offer application.', 'As per offer terms. Payment on delivery.', 2500.00, DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 67 DAY), 'DRAFT', NULL, NOW()),
(3, 4, NULL, NULL, 'Full Stack MVP', 'React + Node MVP. 4 weeks delivery.', 'Standard terms. 50/50 payment.', 3500.00, DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 32 DAY), 'COMPLETED', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
(5, 8, 5, NULL, 'DevOps Setup', 'Docker and K8s migration. CI/CD pipeline.', 'Phased delivery. 3 milestones.', 3800.00, NULL, NULL, 'DRAFT', NULL, NOW()),
(1, 9, 7, NULL, 'ML Recommendation', 'Product recommendation engine. Design to deployment.', 'IP and code handover. Support 30 days.', 5500.00, NULL, NULL, 'DRAFT', NULL, NOW()),
(3, 7, 4, 5, 'Logo and Brand', 'Logo design and brand guidelines.', '2 revisions. Final files in Figma and PNG.', 750.00, NULL, NULL, 'PENDING_SIGNATURE', NULL, NOW()),
(5, 10, 9, 9, 'Agile Coaching', 'Scrum master and Agile coaching for 2 teams.', 'Monthly retainer. 3 months initial.', 5400.00, NULL, NULL, 'DRAFT', NULL, NOW()),
(1, 4, NULL, 10, 'Landing Page', 'Marketing landing page. React and responsive.', 'One page. SEO basics included.', 3200.00, DATE_SUB(CURDATE(), INTERVAL 58 DAY), DATE_SUB(CURDATE(), INTERVAL 33 DAY), 'COMPLETED', DATE_SUB(NOW(), INTERVAL 58 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY));

-- Conflicts (contract_id 1-10, one per contract for testing)
INSERT INTO conflicts (contract_id, raised_by_id, reason, description, evidence_url, status, created_at, resolved_at, resolution) VALUES
(1, 1, 'Scope change', 'Client requested additional payment module not in original scope.', 'Scope document attached.', 'OPEN', NOW(), NULL, NULL),
(2, 3, 'Delivery delay', 'Minor delay on final delivery. Discussing compensation.', NULL, 'IN_REVIEW', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL),
(3, 5, 'Resolved amicably', 'Initial dispute on final deliverable. Resolved with small extra revision.', NULL, 'RESOLVED', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW(), 'Extra revision delivered. Both parties satisfied.'),
(4, 1, 'Payment terms', 'Clarification needed on milestone dates.', NULL, 'OPEN', NOW(), NULL, NULL),
(5, 3, 'N/A', 'No conflict. Completed successfully.', NULL, 'RESOLVED', DATE_SUB(NOW(), INTERVAL 50 DAY), DATE_SUB(NOW(), INTERVAL 50 DAY), 'N/A'),
(6, 8, 'Resource availability', 'Freelancer availability changed. Need new timeline.', NULL, 'OPEN', NOW(), NULL, NULL),
(7, 9, 'Technical scope', 'ML model performance metrics need to be defined.', NULL, 'IN_REVIEW', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
(8, 3, 'Design revisions', 'Number of revisions exceeded. Discussing extra fee.', NULL, 'OPEN', NOW(), NULL, NULL),
(9, 10, 'Schedule', 'Coaching schedule conflict with client holidays.', NULL, 'OPEN', NOW(), NULL, NULL),
(10, 1, 'N/A', 'Project completed. No conflict.', NULL, 'RESOLVED', DATE_SUB(NOW(), INTERVAL 55 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY), 'Completed without issues.');

-- -----------------------------------------------------------------------------
-- 6. REVIEW DB (reviewdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS reviewdb;
USE reviewdb;

INSERT INTO reviews (reviewer_id, reviewee_id, project_id, rating, comment, created_at) VALUES
(1, 2, 1, 5, 'Excellent Angular work. Delivered on time and very responsive.', NOW()),
(3, 4, 3, 5, 'Great dashboard. Clean code and good documentation.', NOW()),
(5, 6, 6, 4, 'Good API integration. Minor delays but good quality.', NOW()),
(1, 7, 4, 5, 'Beautiful logo and brand. Very professional.', NOW()),
(3, 4, 7, 5, 'MVP was exactly what we needed. Strongly recommend.', NOW()),
(5, 8, 5, 4, 'Solid DevOps setup. Team is happy with the pipeline.', NOW()),
(1, 9, 7, 5, 'Recommendation engine works well. Good communication.', NOW()),
(3, 10, 8, 5, 'Agile coaching transformed our team. Thank you!', NOW()),
(5, 2, 2, 4, 'Good backend work. Would work again.', NOW()),
(1, 4, 10, 5, 'Landing page was perfect. Fast and on budget.', NOW());

INSERT INTO review_responses (review_id, respondent_id, message, responded_at) VALUES
(1, 2, 'Thank you! Happy to work with you again.', NOW()),
(2, 4, 'Thanks a lot. Glad the dashboard met your expectations.', NOW()),
(3, 6, 'Thank you. I will work on improving delivery time.', NOW()),
(4, 7, 'Thank you for the kind words!', NOW()),
(5, 4, 'It was a pleasure. Let me know if you need anything else.', NOW()),
(6, 8, 'Thanks. Happy to support the team.', NOW()),
(7, 9, 'Thank you. The model is performing well in production.', NOW()),
(8, 10, 'Glad the coaching helped. Best of luck!', NOW()),
(9, 2, 'Thank you. Looking forward to the next project.', NOW()),
(10, 4, 'Thank you! Quick and smooth collaboration.', NOW());

-- -----------------------------------------------------------------------------
-- 7. PLANNING DB (planningdb)
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS planningdb;
USE planningdb;

INSERT INTO progress_update (project_id, contract_id, freelancer_id, title, description, progress_percentage, created_at, updated_at) VALUES
(1, 1, 2, 'Setup and architecture', 'Repo created, Angular app scaffold, routing and auth module.', 20, NOW(), NOW()),
(2, NULL, 2, 'API design', 'OpenAPI spec and first endpoints.', 15, NOW(), NOW()),
(3, 2, 4, 'Charts integration', 'Recharts integrated. First 3 charts done.', 60, NOW(), NOW()),
(4, 8, 7, 'Moodboard', 'Moodboard and 3 logo concepts sent.', 30, NOW(), NOW()),
(5, NULL, 8, 'Docker base', 'Dockerfiles and docker-compose for dev.', 25, NOW(), NOW()),
(6, 3, 6, 'Completed', 'All endpoints delivered and tested.', 100, NOW(), NOW()),
(7, NULL, 9, 'Data pipeline', 'Data collection and preprocessing done.', 40, NOW(), NOW()),
(8, 9, 10, 'First sprint', 'First sprint retrospective done. Backlog refined.', 33, NOW(), NOW()),
(9, NULL, 6, 'Security review', 'OWASP check and first findings.', 10, NOW(), NOW()),
(10, 10, 4, 'Deployed', 'Landing page live. SEO and analytics added.', 100, NOW(), NOW());

INSERT INTO progress_comment (progress_update_id, user_id, message, created_at) VALUES
(1, 1, 'Looks good. Can we add dark mode in the scope?', NOW()),
(2, 1, 'Please share the API doc when ready.', NOW()),
(3, 3, 'The charts look great. Can we add export to PDF?', NOW()),
(4, 3, 'I prefer concept 2. Can we refine the colors?', NOW()),
(5, 5, 'When do you expect to have staging ready?', NOW()),
(6, 5, 'Thanks. We will run UAT this week.', NOW()),
(7, 1, 'What is the ETA for the first model version?', NOW()),
(8, 3, 'The team is already more organized. Thanks.', NOW()),
(9, 5, 'Keep us posted on critical findings.', NOW()),
(10, 1, 'Perfect. We will share the link with marketing.', NOW());

-- =============================================================================
-- End of seed script
-- =============================================================================
