-- ============================================================
-- Seed: planningdb  (MySQL port 3307)
-- Progress updates and comments submitted by freelancers
-- Run AFTER starting the Planning microservice.
-- ============================================================
USE planningdb;

-- ── Progress Updates ─────────────────────────────────────────
INSERT IGNORE INTO progress_update
  (id, project_id, contract_id, freelancer_id, title, description,
   progress_percentage, created_at, updated_at, next_update_due, github_repo_url)
VALUES
-- ── E-Commerce project (1) — Sarah (2) — contract 1 ──
(1, 1, 1, 2,
   'Week 1: Setup & Architecture Complete',
   'Completed Angular workspace setup, NgRx store configuration, lazy-loaded routing for all modules (catalogue, cart, account, admin), and global HTTP interceptors for JWT and loading indicators. Responsive base layout with Bootstrap 5 theme applied.',
   20, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY),
   DATE_SUB(NOW(), INTERVAL 13 DAY),
   'https://github.com/smartfreelance-demo/ecommerce-frontend'),

(2, 1, 1, 2,
   'Week 2-3: Product Catalogue Module 80% Complete',
   'Product listing grid with server-side pagination, full-text search, multi-faceted filters (category, price range, tags), and product detail page with image carousel. API integration complete. Currently implementing comparison feature requested by client.',
   45, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY),
   DATE_SUB(NOW(), INTERVAL 6 DAY),
   'https://github.com/smartfreelance-demo/ecommerce-frontend'),

(3, 1, 1, 2,
   'Week 4: Catalogue Done, Cart Starting',
   'Product catalogue fully shipped (including compare feature — client approved!). Started shopping cart with NgRx. Stripe test keys integrated; payment form UI complete. Currently wiring checkout steps.',
   62, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
   DATE_ADD(NOW(), INTERVAL 1 DAY),
   'https://github.com/smartfreelance-demo/ecommerce-frontend'),

-- ── Mobile Health App (5) — Elena (6) ──
(4, 5, 3, 6,
   'Week 1: Project Setup & Auth Screens',
   'Expo project initialised with React Native Navigation v6, Zustand for state, and EAS build configured for both iOS and Android targets. Login and registration screens complete with form validation and Keychain-based token storage. Working on forgot-password flow.',
   25, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY),
   DATE_ADD(NOW(), INTERVAL 6 DAY),
   'https://github.com/smartfreelance-demo/health-app-mobile'),

-- ── DevOps Project (3) — James (5) — COMPLETED ──
(5, 3, 2, 5,
   'Phase 1: Terraform IaC Foundation',
   'VPC with public/private subnets, NAT Gateway, ECS cluster, RDS MySQL (Multi-AZ), Application Load Balancer, S3 buckets, and IAM roles all provisioned via Terraform. Staging workspace working; production workspace pending approval.',
   25, DATE_SUB(NOW(), INTERVAL 5 MONTH), DATE_SUB(NOW(), INTERVAL 5 MONTH),
   DATE_SUB(NOW(), INTERVAL 4 MONTH + INTERVAL 15 DAY),
   'https://github.com/smartfreelance-demo/devops-infra'),

(6, 3, 2, 5,
   'Phase 2: Docker & ECR Complete',
   'Multi-stage Dockerfile reduces image size from 800MB to 180MB. ECR repository configured with lifecycle policies. Environment-specific Spring profiles (dev/staging/prod) with Secrets Manager integration for credentials.',
   55, DATE_SUB(NOW(), INTERVAL 4 MONTH), DATE_SUB(NOW(), INTERVAL 4 MONTH),
   DATE_SUB(NOW(), INTERVAL 3 MONTH + INTERVAL 15 DAY),
   'https://github.com/smartfreelance-demo/devops-infra'),

(7, 3, 2, 5,
   'Phase 3: CI/CD Pipeline Live',
   'GitHub Actions pipeline: lint → unit tests → integration tests → Docker build → ECR push → ECS blue/green deploy. Average pipeline duration: 8 minutes. Staging environment successfully running all services.',
   80, DATE_SUB(NOW(), INTERVAL 3 MONTH), DATE_SUB(NOW(), INTERVAL 3 MONTH),
   DATE_SUB(NOW(), INTERVAL 2 MONTH + INTERVAL 10 DAY),
   'https://github.com/smartfreelance-demo/devops-infra'),

(8, 3, 2, 5,
   'Phase 4: Production Go-Live — Project Complete!',
   'Blue/green production deployment successful. Zero downtime. CloudWatch dashboards configured for CPU, memory, request latency, and error rates. PagerDuty integration for critical alerts. Infrastructure cost: $1,840/month vs previous $2,950 (38% saving). Full runbook handed over. Project signed off by client.',
   100, DATE_SUB(NOW(), INTERVAL 1 MONTH + INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 MONTH + INTERVAL 5 DAY),
   NULL,
   'https://github.com/smartfreelance-demo/devops-infra');

-- ── Progress Comments ─────────────────────────────────────────
-- Columns: progress_update_id (FK @JoinColumn), user_id, message, created_at
INSERT IGNORE INTO progress_comment
  (progress_update_id, user_id, message, created_at)
VALUES
-- Comments on Sarah's update #2
(2, 7,
   'Looks amazing Sarah! The filter panel is exactly what we envisioned. The compare feature will be a great addition — make it stand out visually.',
   DATE_SUB(NOW(), INTERVAL 12 DAY)),
(2, 2,
   'Will do! I''m thinking a floating compare tray at the bottom, similar to major e-commerce sites. Will share a Figma mock before implementing.',
   DATE_SUB(NOW(), INTERVAL 12 DAY)),

-- Comments on Sarah's update #3
(3, 7,
   'Love the compare tray! Stripe integration looks clean. One thing: can we also support PayPal checkout as a secondary option?',
   DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 2,
   'Yes, Stripe supports PayPal as a payment method natively — I can enable it with a configuration change. Will add it this week.',
   DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Comments on DevOps final update
(8, 9,
   'Incredible work James. The 38% cost reduction is better than we hoped. The monitoring dashboards are very professional. Signing off with full satisfaction!',
   DATE_SUB(NOW(), INTERVAL 1 MONTH + INTERVAL 3 DAY)),
(8, 5,
   'Thank you David! It was a great project. The runbook is in Confluence. Remember to review the AWS Cost Explorer weekly — I''ve set up a budget alert at $2,200 too.',
   DATE_SUB(NOW(), INTERVAL 1 MONTH + INTERVAL 2 DAY));
