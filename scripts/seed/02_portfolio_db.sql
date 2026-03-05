-- ============================================================
-- Seed: portfolio_db
-- Skills, experiences, and profile views for 5 freelancers (IDs 2-6)
-- Run AFTER starting the Portfolio microservice.
-- ============================================================
USE portfolio_db;

-- ── Experiences ─────────────────────────────────────────────
INSERT IGNORE INTO experiences (id, user_id, title, type, domain, description, start_date, end_date, company_or_client_name) VALUES
-- Sarah Chen (2) - Frontend/Angular
(1,  2, 'Angular Developer',                    'JOB',     'WEB_DEVELOPMENT', 'Developed enterprise Angular applications with NgRx state management for 10k+ daily users.', '2022-01-01', '2023-12-31', 'TechCorp Global'),
(2,  2, 'Freelance E-Commerce Frontend',        'PROJECT',  'WEB_DEVELOPMENT', 'Built a fully responsive Angular e-commerce site with Stripe integration.', '2024-01-01', '2024-06-30', 'ShopStart Inc'),
-- Marco Rivera (3) - Java Backend
(3,  3, 'Java Microservices Engineer',          'JOB',     'WEB_DEVELOPMENT', 'Designed and maintained Spring Boot microservices serving 2M+ API calls daily.', '2021-03-01', '2024-02-28', 'FinTech Solutions'),
(4,  3, 'Open Source Spring Boot Contributor',  'PROJECT',  'WEB_DEVELOPMENT', 'Contributed 20+ pull requests to popular Spring Boot ecosystem libraries.', '2023-06-01', NULL,         'GitHub Open Source'),
-- Aisha Patel (4) - UI/UX
(5,  4, 'Senior UX Designer',                   'JOB',     'UI_UX_DESIGN',    'Led UX research and shipped interface designs for 8 product launches.', '2021-07-01', '2023-08-31', 'DesignLab Agency'),
(6,  4, 'Healthcare App Redesign',              'PROJECT',  'UI_UX_DESIGN',    'Complete Figma redesign of patient portal; improved NPS by 32 points.', '2024-01-01', '2024-04-30', 'MedCare Inc'),
-- James Kim (5) - DevOps
(7,  5, 'Senior DevOps Engineer',               'JOB',     'DEVOPS',          'Managed 40-node Kubernetes cluster and 15 CI/CD pipelines on AWS.', '2020-05-01', '2024-01-31', 'CloudBase Systems'),
(8,  5, 'Cloud Migration Project',              'PROJECT',  'CLOUD_COMPUTING', 'Migrated a monolithic e-retail platform to AWS ECS; cut infra cost by 35%.', '2023-10-01', '2024-03-31', 'E-Retail Co'),
-- Elena Vasquez (6) - Mobile
(9,  6, 'React Native Developer',               'JOB',     'MOBILE_DEV',      'Built and shipped 6 cross-platform apps for iOS & Android (50k+ downloads).', '2022-04-01', '2023-11-30', 'MobileFirst Studio'),
(10, 6, 'FitLife Health Tracker App',           'PROJECT',  'MOBILE_DEV',      'React Native health app with wearable BLE integration; 4.8★ on App Store.', '2024-02-01', '2024-07-31', 'FitLife Startup');

-- ── Skills ──────────────────────────────────────────────────
INSERT IGNORE INTO skills (id, name, description, user_id, experience_id, created_at, updated_at) VALUES
-- Sarah Chen (2)
(1,  'Angular',       'Expert in Angular 17+, NgRx, RxJS, and standalone components.',   2, 1, NOW(), NOW()),
(2,  'TypeScript',    'Advanced TypeScript with generics, decorators, and strict mode.',  2, 1, NOW(), NOW()),
(3,  'React',         'React 18 with hooks, Context API, and React Query.',               2, 2, NOW(), NOW()),
(4,  'CSS / SCSS',    'Responsive design, animations, and component design systems.',     2, NULL, NOW(), NOW()),
-- Marco Rivera (3)
(5,  'Java',          'Java 17 with modern features; clean architecture patterns.',       3, 3, NOW(), NOW()),
(6,  'Spring Boot',   'Spring Boot 3 microservices, security, and data JPA.',            3, 3, NOW(), NOW()),
(7,  'MySQL',         'Schema design, query optimisation, and Hibernate tuning.',         3, 3, NOW(), NOW()),
(8,  'Docker',        'Multi-stage builds, Compose, and private registries.',             3, 4, NOW(), NOW()),
-- Aisha Patel (4)
(9,  'Figma',         'End-to-end wireframes, prototypes, and design systems in Figma.', 4, 5, NOW(), NOW()),
(10, 'Adobe XD',      'High-fidelity mockups and interactive prototypes.',                4, 5, NOW(), NOW()),
(11, 'UX Research',   'User interviews, usability testing, and journey mapping.',         4, 6, NOW(), NOW()),
(12, 'Sketch',        'Sketch + Zeplin design handoff workflow.',                         4, NULL, NOW(), NOW()),
-- James Kim (5)
(13, 'Docker',        'Advanced orchestration, multi-stage builds, and security.',        5, 7, NOW(), NOW()),
(14, 'Kubernetes',    'Production K8s: Helm, Istio, HPA, RBAC.',                         5, 7, NOW(), NOW()),
(15, 'AWS',           'EC2, ECS, Lambda, S3, RDS, CloudFormation, and Cost Explorer.',   5, 8, NOW(), NOW()),
(16, 'CI/CD',         'Jenkins, GitHub Actions, GitLab CI — from commit to production.', 5, 7, NOW(), NOW()),
-- Elena Vasquez (6)
(17, 'React Native',  'Expo and bare React Native with Navigation and Reanimated.',       6, 9, NOW(), NOW()),
(18, 'Flutter',       'Dart/Flutter with BLoC pattern for iOS and Android.',              6, 9, NOW(), NOW()),
(19, 'Kotlin/Android','Native Android with Jetpack Compose and Room.',                   6, 10, NOW(), NOW()),
(20, 'Swift/iOS',     'SwiftUI and UIKit for iPhone and iPad applications.',              6, 10, NOW(), NOW());

-- ── Skill domains ───────────────────────────────────────────
INSERT IGNORE INTO skill_domains (skill_id, domain) VALUES
(1, 'WEB_DEVELOPMENT'), (2, 'WEB_DEVELOPMENT'), (3, 'WEB_DEVELOPMENT'), (4, 'UI_UX_DESIGN'),
(5, 'WEB_DEVELOPMENT'), (6, 'WEB_DEVELOPMENT'), (7, 'DATABASE_ADMINISTRATION'), (8, 'DEVOPS'),
(9, 'UI_UX_DESIGN'), (10, 'UI_UX_DESIGN'), (11, 'UI_UX_DESIGN'), (12, 'UI_UX_DESIGN'),
(13, 'DEVOPS'), (14, 'DEVOPS'), (15, 'CLOUD_COMPUTING'), (16, 'DEVOPS'),
(17, 'MOBILE_DEV'), (18, 'MOBILE_DEV'), (19, 'MOBILE_DEV'), (20, 'MOBILE_DEV');

-- ── Experience ↔ Skill links ─────────────────────────────────
INSERT IGNORE INTO experience_skills (experience_id, skill_id) VALUES
(1,1),(1,2),(2,3),(2,4),
(3,5),(3,6),(3,7),(4,8),
(5,9),(5,10),(6,11),
(7,13),(7,14),(7,16),(8,15),
(9,17),(9,18),(10,19),(10,20);

-- ── Profile views ────────────────────────────────────────────
-- Clients (7,8,9) browsing freelancer profiles over the last 30 days
INSERT IGNORE INTO profile_views (profile_user_id, viewer_id, view_date, viewed_at) VALUES
-- Alex (7) viewing Sarah (2)
(2, 7, CURDATE() - INTERVAL 14 DAY, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(2, 7, CURDATE() - INTERVAL 7 DAY,  DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 7, CURDATE() - INTERVAL 3 DAY,  DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 7, CURDATE() - INTERVAL 1 DAY,  DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- Maria (8) viewing Sarah (2)
(2, 8, CURDATE() - INTERVAL 10 DAY, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 8, CURDATE() - INTERVAL 2 DAY,  DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- David (9) viewing Marco (3)
(3, 9, CURDATE() - INTERVAL 20 DAY, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3, 9, CURDATE() - INTERVAL 12 DAY, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(3, 9, CURDATE() - INTERVAL 5 DAY,  DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- Alex (7) viewing Marco (3)
(3, 7, CURDATE() - INTERVAL 8 DAY,  DATE_SUB(NOW(), INTERVAL 8 DAY)),
-- Maria (8) viewing Aisha (4)
(4, 8, CURDATE() - INTERVAL 18 DAY, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(4, 8, CURDATE() - INTERVAL 6 DAY,  DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 8, CURDATE() - INTERVAL 1 DAY,  DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- David (9) viewing James (5)
(5, 9, CURDATE() - INTERVAL 25 DAY, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(5, 9, CURDATE() - INTERVAL 15 DAY, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(5, 9, CURDATE() - INTERVAL 4 DAY,  DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- Alex (7) viewing Elena (6)
(6, 7, CURDATE() - INTERVAL 11 DAY, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(6, 7, CURDATE() - INTERVAL 3 DAY,  DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- Maria (8) viewing Elena (6)
(6, 8, CURDATE() - INTERVAL 9 DAY,  DATE_SUB(NOW(), INTERVAL 9 DAY)),
-- Anonymous views
(2, NULL, CURDATE() - INTERVAL 22 DAY, DATE_SUB(NOW(), INTERVAL 22 DAY)),
(3, NULL, CURDATE() - INTERVAL 16 DAY, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4, NULL, CURDATE() - INTERVAL 13 DAY, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(5, NULL, CURDATE() - INTERVAL 7 DAY,  DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6, NULL, CURDATE() - INTERVAL 4 DAY,  DATE_SUB(NOW(), INTERVAL 4 DAY));
