-- ============================================================
-- Seed: reviewdb
-- Reviews written after completed projects
-- Run AFTER starting the Review microservice.
-- ============================================================
USE reviewdb;

-- ── Reviews ──────────────────────────────────────────────────
-- reviewer_id → person writing the review
-- reviewee_id → person being reviewed
-- project_id  → project #3 (completed DevOps, David↔James)
INSERT IGNORE INTO reviews
  (id, reviewer_id, reviewee_id, project_id, rating, comment, created_at)
VALUES
-- David (9) reviews James (5) — client reviewing freelancer
(1, 9, 5, 3, 5,
   'James exceeded every expectation. The AWS migration was complex but he handled everything professionally. Our infrastructure costs dropped by 38% and we''ve had zero downtime since go-live. Highly recommend!',
   DATE_SUB(NOW(), INTERVAL 25 DAY)),

-- James (5) reviews David (9) — freelancer reviewing client
(2, 5, 9, 3, 5,
   'David was an outstanding client — clear requirements, quick decisions, and prompt payment at each milestone. The project had a minor delay but we resolved it together like true professionals. Would love to work with him again.',
   DATE_SUB(NOW(), INTERVAL 24 DAY)),

-- Alex (7) reviews Sarah (2) — interim review (project still in progress but partial work delivered)
(3, 7, 2, 1, 5,
   'Sarah is exactly the Angular developer we needed. Halfway through the project and her code quality, communication, and on-time delivery have been impeccable. The Angular components she built are clean and well-documented.',
   DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ── Review Responses ─────────────────────────────────────────
INSERT IGNORE INTO review_responses
  (id, review_id, respondent_id, message, responded_at)
VALUES
-- James responds to David's 5-star review
(1, 1, 5,
   'Thank you so much, David! It was a pleasure working with your team. The AWS migration was challenging but highly rewarding. I hope our paths cross on future projects!',
   DATE_SUB(NOW(), INTERVAL 24 DAY)),

-- Sarah responds to Alex's review
(2, 3, 2,
   'Thank you, Alex! I really enjoy building the e-commerce platform — the product requirements are well thought-out which makes my job easier. Looking forward to the final delivery!',
   DATE_SUB(NOW(), INTERVAL 4 DAY));
