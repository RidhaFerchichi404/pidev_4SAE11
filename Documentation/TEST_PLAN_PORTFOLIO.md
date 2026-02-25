# Portfolio Management - Use Case Test Plan

This document outlines the test scenarios for the Portfolio Management module, including Experience tracking, Skills management, and AI-based Skill Verification.

## 1. Managing Experiences

**Goal**: Verify a freelancer can build their career timeline.

| ID | Scenario | Steps | Expected Result | Status |
|----|----------|-------|-----------------|--------|
| **EXP-01** | **View Empty State** | 1. Open "My Portfolio" page as a new user. | "Career History" section shows "No experiences added yet". "My Skills" section is empty. | [OK] |
| **EXP-02** | **Add Job Experience** | 1. Click "+ Add Experience".<br>2. Fill form: Title="Frontend Dev", Company="TechCorp", Type="JOB", Dates, Description.<br>3. Add Key Task="Built login page".<br>4. Add Skill="Angular".<br>5. Click Save. | Modal closes. Experience appears in timeline. "Angular" appears in the "My Skills" section automatically. | [KO] |
| **EXP-03** | **Add Project Experience** | 1. Add another experience.<br>2. Type="PROJECT".<br>3. Add Skill="Spring Boot".<br>4. Save. | Experience added. "Spring Boot" added to skills list. | [KO] |
| **EXP-04** | **Edit Experience** | 1. Click "Edit" on the first experience.<br>2. Change Title to "Senior Frontend Dev".<br>3. Add new skill "TypeScript".<br>4. Save. | Card updates with new title. "TypeScript" is added to the Skills list. | [KO] |
| **EXP-05** | **Delete Experience** | 1. Click "Delete" on the project experience.<br>2. Confirm. | Experience disappears from timeline. The skill "Spring Boot" remains in the skills list. | [KO] |

## 2. Managing Skills (Explicit)

**Goal**: Verify direct skill management independent of experiences.

| ID | Scenario | Steps | Expected Result | Status |
|----|----------|-------|-----------------|--------|
| **SKL-01** | **Add New Skill** | 1. In "My Skills" section, click "Add Skill".<br>2. Enter "Docker".<br>3. Save. | "Docker" appears in the grid with "Unverified" badge. | [KO] |
| **SKL-02** | **Delete Skill** | 1. Click "Delete" on "Docker".<br>2. Confirm. | "Docker" disappears from the grid. | [KO] |
| **SKL-03** | **Delete Used Skill** | 1. Try to delete "Angular" (linked to "Frontend Dev" experience). | Skill is deleted. Check experience card: "Angular" tag should be gone (or system handles it gracefully). | [KO] |

## 3. AI Skill Verification

**Goal**: Verify the AI testing flow.

| ID | Scenario | Steps | Expected Result | Status |
|----|----------|-------|-----------------|--------|
| **VER-01** | **Generate Test** | 1. Click "Verify" on "Angular". | "Generating your test..." loading state appears. Then, a quiz modal opens with a question about Angular. | [KO] |
| **VER-02** | **Take Test (Pass Flow)** | 1. Select correct answers (green feedback immediately).<br>2. Click Next/Finish. | Result screen shows "Passed". Score is displayed. | [KO] |
| **VER-03** | **Take Test (Fail Flow)** | 1. Verify another skill.<br>2. Select wrong answers (red feedback immediately).<br>3. Finish. | Result screen shows "Failed". | [KO] |

## 4. Technical Validation

**Goal**: Verify error handling and system resilience.

| ID | Scenario | Steps | Expected Result | Status |
|----|----------|-------|-----------------|--------|
| **TEC-01** | **Backend Down** | 1. Stop the Portfolio service.<br>2. Try to load the page. | Error message in console (502/500). UI might show empty state or error toast (depending on implementation). | [OK] |
| **TEC-02** | **Invalid API Key** | 1. Set an invalid key in `.env`.<br>2. Try "Verify". | "Failed to generate test" alert/message appears. | [OK] |
