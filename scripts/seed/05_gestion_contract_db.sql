-- ============================================================
-- Seed: gestion_contract_db
-- Contracts and conflicts between clients and freelancers
-- Run AFTER starting the Contract microservice.
-- ============================================================
USE gestion_contract_db;

-- ── Contracts ────────────────────────────────────────────────
INSERT IGNORE INTO contracts
  (id, client_id, freelancer_id, project_application_id, offer_application_id,
   title, description, terms, amount, start_date, end_date, status, signed_at, created_at)
VALUES
-- Contract 1: Alex (7) ↔ Sarah (2) for E-Commerce project — SIGNED / ACTIVE
(1, 7, 2, 1, 1,
   'E-Commerce Platform Development Contract',
   'Sarah Chen will develop the Angular frontend for the B2B E-Commerce Platform (project #1), including all UI components, NgRx state management, and API integration.',
   'Payment: $14,500 total — $4,000 on kick-off, $5,000 at 50% milestone, $5,500 on final delivery. Revisions: 2 rounds included. IP transfers to client on final payment. NDA applies.',
   14500.00, CURDATE() - INTERVAL 20 DAY, CURDATE() + INTERVAL 100 DAY,
   'SIGNED', DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY)),

-- Contract 2: David (9) ↔ James (5) for DevOps project — COMPLETED
(2, 9, 5, 3, 3,
   'AWS DevOps Migration & CI/CD Contract',
   'James Kim will migrate the client''s monolithic Java application to AWS ECS and set up full CI/CD automation with GitHub Actions and Terraform.',
   'Payment: $19,000 total — $5,000 upfront, $7,000 at migration milestone, $7,000 on sign-off. SLA: 99.9% uptime post-migration. 60-day support warranty included.',
   19000.00, CURDATE() - INTERVAL 5 MONTH, CURDATE() - INTERVAL 1 MONTH,
   'COMPLETED', DATE_SUB(NOW(), INTERVAL 5 MONTH), DATE_SUB(NOW(), INTERVAL 5 MONTH + INTERVAL 2 DAY)),

-- Contract 3: Maria (8) ↔ Elena (6) for Mobile App — DRAFT (not yet signed)
(3, 8, 6, 5, 5,
   'Patient Mobile App Development Contract',
   'Elena Vasquez will develop the iOS and Android patient mobile application using React Native, including BLE wearable integration and HIPAA-compliant data handling.',
   'Payment: $17,500 total — $4,500 on kick-off, $6,500 at beta milestone, $6,500 on App Store approval. App source code and assets transfer to client on completion.',
   17500.00, CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 4 MONTH,
   'DRAFT', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ── Conflicts ────────────────────────────────────────────────
-- One resolved conflict on the completed DevOps contract
INSERT IGNORE INTO conflicts
  (id, contract_id, raised_by_id, reason, description, status, created_at, resolved_at, resolution)
VALUES
(1, 2, 9,
   'Delayed delivery',
   'The initial infrastructure setup was delayed by 2 weeks due to AWS account permission issues. This pushed the migration milestone past the agreed date.',
   'RESOLVED',
   DATE_SUB(NOW(), INTERVAL 3 MONTH),
   DATE_SUB(NOW(), INTERVAL 2 MONTH + INTERVAL 15 DAY),
   'Both parties agreed to a 2-week timeline extension at no additional cost. James provided a detailed post-mortem and the migration was completed successfully.');
