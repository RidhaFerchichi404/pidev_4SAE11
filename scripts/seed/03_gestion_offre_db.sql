-- ============================================================
-- Seed: gestion_offre_db
-- Offers posted by freelancers + applications from clients
-- Run AFTER starting the Offer microservice.
-- ============================================================
USE gestion_offre_db;

-- ── Offers (posted by freelancers) ──────────────────────────
-- Note: SpringPhysicalNamingStrategy maps camelCase → snake_case.
-- Explicit @Column(name="offerStatus") still goes through the
-- strategy, so it becomes offer_status in the DB.
INSERT IGNORE INTO offers
  (id, freelancer_id, title, domain, description, price, duration_type,
   offer_status, deadline, category, rating, communication_score,
   tags, views_count, is_featured, is_active, created_at, updated_at, published_at)
VALUES
(1, 2, 'Angular & React Frontend Development',
   'Web Development',
   'I build fast, accessible, and beautiful web frontends using Angular 17+ and React 18. Experienced with NgRx, RxJS, REST/GraphQL APIs, and responsive design. I deliver pixel-perfect UIs on time.',
   85.00, 'hourly',
   'AVAILABLE', '2026-12-31', 'Frontend Development', 4.80, 4.90,
   'Angular,React,TypeScript,NgRx,RxJS,Responsive', 142, 1, 1, NOW(), NOW(), NOW()),

(2, 3, 'Java Spring Boot Microservices Backend',
   'Backend Development',
   'Full-stack Java developer specialising in Spring Boot 3 microservices. I design RESTful APIs, integrate databases with Hibernate, handle security with JWT/Keycloak, and write comprehensive tests.',
   90.00, 'hourly',
   'AVAILABLE', '2026-12-31', 'Backend Development', 4.90, 4.85,
   'Java,SpringBoot,Microservices,MySQL,Docker', 98, 1, 1, NOW(), NOW(), NOW()),

(3, 4, 'UI/UX Design & Figma Prototyping',
   'Design',
   'I create user-centred designs that delight users and achieve business goals. Services include UX research, wireframing, high-fidelity prototypes, and design system creation using Figma and Adobe XD.',
   70.00, 'hourly',
   'AVAILABLE', '2026-12-31', 'UI/UX Design', 4.70, 4.95,
   'Figma,AdobeXD,UXResearch,Prototyping,DesignSystems', 87, 0, 1, NOW(), NOW(), NOW()),

(4, 5, 'DevOps & Cloud Infrastructure (AWS/K8s)',
   'DevOps',
   'I set up robust CI/CD pipelines, Kubernetes clusters, and AWS cloud infrastructure. Specialities: Docker, Helm, Terraform, GitHub Actions, cost optimisation, and zero-downtime deployments.',
   100.00, 'hourly',
   'AVAILABLE', '2026-12-31', 'DevOps / Cloud', 4.95, 4.80,
   'DevOps,Kubernetes,AWS,Docker,Terraform,CI/CD', 65, 1, 1, NOW(), NOW(), NOW()),

(5, 6, 'React Native & Flutter Mobile Development',
   'Mobile Development',
   'Cross-platform mobile developer (iOS + Android) using React Native and Flutter. I deliver smooth, native-feeling apps from design to App Store submission, with push notifications and offline support.',
   80.00, 'hourly',
   'AVAILABLE', '2026-12-31', 'Mobile Development', 4.75, 4.85,
   'ReactNative,Flutter,iOS,Android,Expo,BLE', 54, 0, 1, NOW(), NOW(), NOW());

-- ── Offer Applications (clients applying to freelancers' offers) ──
INSERT IGNORE INTO offer_applications
  (id, offer_id, client_id, message, proposed_budget, estimated_duration,
   status, is_read, applied_at)
VALUES
-- Alex (7) applying to Sarah's offer (1)
(1, 1, 7,
   'Hi Sarah, we are building a B2B e-commerce platform and need a skilled Angular developer. Your portfolio looks excellent! We have a 6-month project starting next month. Can we schedule a call?',
   80.00, 180, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),

-- Maria (8) applying to Marco's offer (2)
(2, 2, 8,
   'Hello Marco, we need a Spring Boot developer to build the backend APIs for our health monitoring mobile app. JWT auth, REST APIs, and MySQL. Are you available for a 3-month engagement?',
   85.00, 90, 'PENDING', 0, DATE_SUB(NOW(), INTERVAL 10 DAY)),

-- David (9) applying to James's offer (4)
(3, 4, 9,
   'James, we need DevOps help migrating our monolith to AWS ECS with CI/CD pipelines. Timeline: 4 months. Our stack is Java/Spring. You seem like the perfect fit!',
   95.00, 120, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 35 DAY)),

-- Alex (7) applying to Aisha's offer (3)
(4, 3, 7,
   'Aisha, we want to redesign our SaaS dashboard with a modern look. We have existing Figma files as a starting point. This is a 2-month project. Interested?',
   65.00, 60, 'PENDING', 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- Maria (8) applying to Elena's offer (5)
(5, 5, 8,
   'Elena, we are building a patient-facing mobile health tracker. React Native preferred. The app needs BLE for wearables. 4-month project. Your FitLife portfolio is impressive!',
   75.00, 120, 'ACCEPTED', 1, DATE_SUB(NOW(), INTERVAL 15 DAY));
