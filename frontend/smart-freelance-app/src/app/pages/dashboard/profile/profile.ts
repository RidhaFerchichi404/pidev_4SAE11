import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PortfolioService, Skill, Experience, EvaluationResult } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
  standalone: true,
})
export class Profile implements OnInit {
  skills: Skill[] = [];
  experiences: Experience[] = [];
  evaluations: EvaluationResult[] = [];
  isLoading = true;

  activeTab: 'skills' | 'experience' | 'evaluations' = 'skills';

  constructor(
    public auth: AuthService,
    private portfolioService: PortfolioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const userId = this.auth.getUserId();
    if (!userId) {
      this.isLoading = false;
      return;
    }

    if (this.auth.isFreelancer()) {
      let pending = 3;
      const done = () => { if (--pending === 0) { this.isLoading = false; this.cdr.detectChanges(); } };

      this.portfolioService.getUserSkills(userId).subscribe({
        next: (s) => { this.skills = Array.isArray(s) ? s : []; done(); },
        error: () => { this.skills = []; done(); }
      });
      this.portfolioService.getExperiences(userId).subscribe({
        next: (e) => { this.experiences = Array.isArray(e) ? e : []; done(); },
        error: () => { this.experiences = []; done(); }
      });
      this.portfolioService.getUserEvaluations(userId).subscribe({
        next: (ev) => {
          const arr = Array.isArray(ev) ? ev : [];
          this.evaluations = arr.sort((a, b) =>
            new Date(b.evaluatedAt || '').getTime() - new Date(a.evaluatedAt || '').getTime()
          );
          done();
        },
        error: () => { this.evaluations = []; done(); }
      });
    } else {
      this.isLoading = false;
    }
  }

  // ── Computed display values ───────────────────────────────

  get displayName(): string {
    return this.auth.getDisplayName();
  }

  get username(): string {
    return this.auth.getPreferredUsername() || this.displayName.toLowerCase().replace(' ', '.');
  }

  get role(): string {
    return this.auth.getUserRole() || 'USER';
  }

  get initials(): string {
    const parts = this.displayName.trim().split(' ');
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return this.displayName.slice(0, 2).toUpperCase();
  }

  // ── Skill stats ───────────────────────────────────────────

  get verifiedSkills(): Skill[] {
    return this.skills.filter(s => s.verified);
  }

  get skillDomains(): string[] {
    const domains = [...new Set(this.skills.flatMap(s => (s.domains && s.domains.length > 0) ? s.domains : ['Other']))];
    return domains.sort();
  }

  getSkillsByDomain(domain: string): Skill[] {
    return this.skills.filter(s => {
      const d = (s.domains && s.domains.length > 0) ? s.domains[0] : 'Other';
      return d === domain;
    });
  }

  get avgScore(): number {
    const scored = this.evaluations.filter(e => e.score > 0);
    if (!scored.length) return 0;
    return Math.round(scored.reduce((sum, e) => sum + e.score, 0) / scored.length);
  }

  get passedCount(): number {
    return this.evaluations.filter(e => e.passed).length;
  }

  get failedCount(): number {
    return this.evaluations.filter(e => !e.passed).length;
  }

  get passRate(): number {
    if (!this.evaluations.length) return 0;
    return Math.round((this.passedCount / this.evaluations.length) * 100);
  }

  // ── Styling helpers ────────────────────────────────────────

  getSkillColor(name: string): number {
    const code = name ? name.charCodeAt(0) : 0;
    return (code % 6) + 1;
  }

  getExperienceIcon(type: string): string {
    return type === 'JOB' ? '💼' : '🚀';
  }

  getDomainIcon(domain: string): string {
    const map: Record<string, string> = {
      'Frontend':  '🎨',
      'Backend':   '⚙️',
      'DevOps':    '🛠️',
      'Database':  '🗃️',
      'Data':      '📊',
      'Mobile':    '📱',
      'Security':  '🔐',
      'Cloud':     '☁️',
      'AI':        '🤖',
      'Testing':   '🧪',
      'General':   '💡',
      'Other':     '💡',
    };
    return map[domain] || '📦';
  }

  getSkillLevel(score?: number): string {
    if (score === undefined || score === null) return 'Unrated';
    if (score >= 80) return 'Expert';
    if (score >= 60) return 'Intermediate';
    return 'Beginner';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString('en-GB', { month: 'short', year: 'numeric' });
  }

  formatFullDate(dateStr?: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  setTab(tab: 'skills' | 'experience' | 'evaluations') {
    this.activeTab = tab;
  }
}
