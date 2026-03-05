#!/bin/bash
# ============================================================
# seed-all.sh — Load fake seed data into all microservice DBs
#
# PREREQUISITES:
#   1. MySQL running on port 3306 (root, no password) — ALL services
#   2. All microservices started at least once so Hibernate
#      creates the tables (ddl-auto=update).
#
# USERS (password for all: "password123"):
#   ID 1  admin@smartfreelance.com  ADMIN
#   ID 2  sarah.chen@email.com      FREELANCER  (Angular)
#   ID 3  marco.rivera@email.com    FREELANCER  (Java)
#   ID 4  aisha.patel@email.com     FREELANCER  (UI/UX)
#   ID 5  james.kim@email.com       FREELANCER  (DevOps)
#   ID 6  elena.vasquez@email.com   FREELANCER  (Mobile)
#   ID 7  alex.johnson@email.com    CLIENT
#   ID 8  maria.garcia@email.com    CLIENT
#   ID 9  david.wilson@email.com    CLIENT
# ============================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MYSQL="mysql -u root --password="

run() {
  local file="$1"
  local port="${2:-3306}"
  echo "  → Running $(basename "$file") on port $port..."
  $MYSQL -P "$port" < "$file"
}

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Smart Freelance — Seed Data Loader"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

run "$DIR/01_userdb.sql"          3306
run "$DIR/02_portfolio_db.sql"    3306
run "$DIR/03_gestion_offre_db.sql" 3306
run "$DIR/04_projectdb.sql"       3306
run "$DIR/05_gestion_contract_db.sql" 3306
run "$DIR/06_reviewdb.sql"        3306
run "$DIR/07_taskdb.sql"          3306
run "$DIR/08_planningdb.sql"      3306

echo ""
echo "✓ All seed data loaded successfully!"
echo ""
echo "Quick summary:"
echo "  userdb              → 9 users (1 admin, 5 freelancers, 3 clients)"
echo "  portfolio_db        → 10 experiences, 20 skills, 24 profile views"
echo "  gestion_offre_db    → 5 offers, 5 applications"
echo "  projectdb           → 5 projects, 6 applications"
echo "  gestion_contract_db → 3 contracts, 1 conflict"
echo "  reviewdb            → 3 reviews, 2 responses"
echo "  taskdb              → 12 tasks, 5 comments"
echo "  planningdb          → 8 progress updates, 6 comments"
echo ""
