# Implementation Specification: Career History & My Skills Redesign

This document details the technical implementation steps for redesigning the "Career History" and "My Skills" sections of the Smart Freelance App, based on the [UI/UX Redesign Spec](../UI_UX_Redesign_Spec.md).

## 1. Global Styles Update
**Target File:** `frontend/smart-freelance-app/src/styles.scss`

The following CSS variables need to be updated or added to support the new design system tokens.

```scss
:root {
  /* Updated Palette */
  --primary: #E37E33;         /* Was #EAC3A9 - Now darker orange */
  --primary-light: #FCE8D6;   /* New tint */
  --primary-dark: #C25E12;    /* Darker shade for active states */
  
  --secondary: #E23D59;       /* Retained */
  --bg-base: #F8F9FA;         /* Neutral-50 */
  
  /* Text Colors */
  --text-primary: #111827;    /* Neutral-900 */
  --text-secondary: #4B5563;  /* Neutral-600 */
  --text-muted: #6B7280;      /* Neutral-500 */
  
  /* Spacing & Radius */
  --card-padding: 24px;       /* $spacing-lg */
  --card-radius: 16px;        /* $radius-xl */
  
  /* Timeline */
  --timeline-width: 2px;
  --timeline-color: #E5E7EB;  /* Neutral-200 */
}
```

---

## 2. Component: Career History (Portfolio Overview)
**Target Files:** 
- `frontend/smart-freelance-app/src/app/pages/dashboard/portfolio-overview/portfolio-overview.html`
- `frontend/smart-freelance-app/src/app/pages/dashboard/portfolio-overview/portfolio-overview.scss`

### 2.1 HTML Structure Changes
Refactor the `.experience-item` loop to support the "Date on Left, Card on Right" layout.

**New Markup Structure:**
```html
<div class="timeline-container">
  <div class="timeline-item" *ngFor="let exp of experiences">
    
    <!-- Left: Date Marker -->
    <div class="timeline-marker">
      <span class="date-label">{{ exp.startDate | date:'MMM yyyy' }} - {{ exp.endDate ? (exp.endDate | date:'MMM yyyy') : 'Present' }}</span>
      <div class="dot"></div>
    </div>

    <!-- Right: Content Card -->
    <app-card class="timeline-card">
      <div class="card-header">
        <div class="role-info">
          <h3>{{ exp.title }}</h3>
          <span class="company">{{ exp.companyOrClientName }}</span>
        </div>
        
        <!-- Actions Dropdown (Top Right) -->
        <div class="actions-menu">
           <!-- Implement a simple dropdown or keep inline if dropdown component unavailable -->
           <button class="icon-btn" (click)="openEditForm(exp)">✎</button>
        </div>
      </div>

      <div class="card-meta">
        <span class="badge">{{ exp.type }}</span>
      </div>

      <div class="card-body">
        <p class="description">{{ exp.description }}</p>
      </div>

      <div class="card-footer" *ngIf="exp.skills?.length">
        <div class="skill-chips-scroll">
          <span class="chip" *ngFor="let skill of exp.skills">{{ skill.name }}</span>
        </div>
      </div>
    </app-card>
  </div>
</div>
```

### 2.2 SCSS Layout Rules
1.  **Grid Layout:** Use CSS Grid on `.timeline-item` to align Date (col 1) and Card (col 2).
2.  **Timeline Line:** Create a continuous line using a pseudo-element on the `.timeline-container` or `.timeline-marker`.
    *   **Desktop:** Date takes ~150px fixed width.
    *   **Mobile:** Stack vertically (Date above Card).

---

## 3. Component: My Skills (Skill Management)
**Target Files:**
- `frontend/smart-freelance-app/src/app/pages/dashboard/skill-management/skill-management.html`
- `frontend/smart-freelance-app/src/app/pages/dashboard/skill-management/skill-management.scss`
- `frontend/smart-freelance-app/src/app/pages/dashboard/skill-management/skill-management.ts` (Logic for grouping)

### 3.1 Logic Updates (.ts)
Update `skills` input to be grouped by `domain`.
*   Create a getter or transform method: `get groupedSkills(): { [domain: string]: Skill[] }`.

### 3.2 HTML Structure Changes
Switch from a flat grid to a Domain-based layout.

**New Markup Structure:**
```html
<div class="skills-container">
  
  <!-- Loop through domains -->
  <div class="domain-section" *ngFor="let domain of skillDomains">
    <h3 class="domain-title">{{ domain }}</h3>
    
    <div class="skills-grid">
      <div class="skill-card-minimal" *ngFor="let skill of getSkillsByDomain(domain)">
        
        <!-- Status Indicator (Color coded dot) -->
        <div class="status-dot" [class.verified]="skill.verified"></div>
        
        <span class="skill-name">{{ skill.name }}</span>
        
        <div class="hover-actions">
           <button class="delete-btn" (click)="confirmDelete(skill.id)">🗑️</button>
        </div>

        <!-- Verified Icon (Visible if verified) -->
        <span class="check-icon" *ngIf="skill.verified">✓</span>
        <!-- Verify Link (Visible if unverified) -->
        <a class="verify-link" *ngIf="!skill.verified" (click)="verifySkill(skill.id)">Verify</a>

      </div>
    </div>
  </div>

</div>
```

### 3.3 SCSS Layout Rules
1.  **Grid:** `grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));` for the cards.
2.  **Card Style:**
    *   Remove large icon.
    *   Reduce padding to `16px`.
    *   Border: `1px solid var(--neutral-200)`.
    *   Hover: `border-color: var(--primary); transform: translateY(-2px);`

---

## 4. Implementation Checklist

### Phase 1: Preparation
- [ ] Backup current `styles.scss` and component files.
- [ ] Update `styles.scss` with new variables.

### Phase 2: Career History
- [ ] Modify `portfolio-overview.html` to implement the timeline layout.
- [ ] Update `portfolio-overview.scss` to style the timeline, dots, and cards.
- [ ] Verify responsiveness (mobile stack vs desktop side-by-side).

### Phase 3: My Skills
- [ ] Update `skill-management.ts` to include logic for grouping skills by domain.
- [ ] Modify `skill-management.html` to render domain sections.
- [ ] Implement the "Minimal Skill Card" design in `skill-management.scss`.

### Phase 4: Review & Polish
- [ ] Check console for errors.
- [ ] Verify hover states and interaction feedback.
- [ ] Ensure no regressions in other parts of the dashboard.
