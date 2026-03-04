import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, TimeoutError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PortfolioService, Skill, EvaluationTest, EvaluationResult, Domain, DOMAIN_LABELS } from '../../../core/services/portfolio.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-skill-management',
  imports: [CommonModule, FormsModule],
  templateUrl: './skill-management.html',
  styleUrl: './skill-management.scss',
  standalone: true,
})
export class SkillManagement implements OnInit, OnDestroy {
  skills: Skill[] = [];
  showAddModal = false;
  showDomainDropdown = false;
  newSkillName = '';

  // Domain checkbox state — pre-seeded from the local DOMAIN_LABELS map so
  // checkboxes render immediately; the backend call in ngOnInit keeps it in sync.
  availableDomains: Domain[] = Object.keys(DOMAIN_LABELS) as Domain[];
  selectedDomains = new Set<Domain>();
  readonly domainLabels = DOMAIN_LABELS;

  // Test State
  showTestModal = false;
  currentTest: EvaluationTest | null = null;
  currentQuestionIndex = 0;
  selectedOption = '';
  testAnswers: { questionIndex: number; selectedOption: string }[] = [];
  testResult: any = null;
  isGeneratingTest = false;
  isAnswerSubmitted = false;
  isAnswerCorrect = false;

  // History State
  showHistoryModal = false;
  evaluationHistory: EvaluationResult[] = [];
  isLoadingHistory = false;

  // Delete Confirmation State
  showDeleteConfirm = false;
  skillToDeleteId: number | null = null;

  // Error State
  errorMessage: string | null = null;
  skillFormErrors: Record<string, string> = {};

  // Subscription management
  private skillsSubscription?: Subscription;

  constructor(
    public auth: AuthService,
    private portfolioService: PortfolioService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadSkills();

    this.skillsSubscription = this.portfolioService.skillsUpdated$.subscribe(() => {
      setTimeout(() => this.loadSkills(), 0);
    });
  }

  ngOnDestroy() {
    this.skillsSubscription?.unsubscribe();
  }

  loadSkills() {
    const userId = this.auth.getUserId() || 1;
    this.portfolioService.getUserSkills(userId).subscribe({
      next: (skills) => {
        this.skills = skills;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading skills:', err);
        this.errorMessage = 'Failed to load skills.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Domain checkbox helpers ─────────────────────────────────

  toggleDomain(domain: Domain) {
    if (this.selectedDomains.has(domain)) {
      this.selectedDomains.delete(domain);
    } else {
      this.selectedDomains.add(domain);
    }
    if (this.skillFormErrors['domains']) this.skillFormErrors['domains'] = '';
  }

  isDomainSelected(domain: Domain): boolean {
    return this.selectedDomains.has(domain);
  }

  // ── Grouping ────────────────────────────────────────────────

  get groupedSkills(): { [domain: string]: Skill[] } {
    return this.skills.reduce((acc, skill) => {
      const primary = (skill.domains && skill.domains.length > 0) ? skill.domains[0] : 'OTHER';
      if (!acc[primary]) acc[primary] = [];
      acc[primary].push(skill);
      return acc;
    }, {} as { [domain: string]: Skill[] });
  }

  get skillDomains(): string[] {
    return Object.keys(this.groupedSkills).sort();
  }

  getSkillsByDomain(domain: string): Skill[] {
    return this.groupedSkills[domain] || [];
  }

  // ── Add Modal ───────────────────────────────────────────────

  getSelectedDomainsArray(): Domain[] {
    return [...this.selectedDomains];
  }

  openAddModal() {
    this.errorMessage = null;
    this.newSkillName = '';
    this.selectedDomains = new Set();
    this.skillFormErrors = {};
    this.showDomainDropdown = false;
    this.showAddModal = true;
  }

  private validateSkillForm(): boolean {
    this.skillFormErrors = {};
    const name = this.newSkillName.trim();

    if (!name) {
      this.skillFormErrors['name'] = 'Skill name is required.';
    } else if (name.length < 2) {
      this.skillFormErrors['name'] = 'Skill name must be at least 2 characters.';
    } else if (name.length > 50) {
      this.skillFormErrors['name'] = 'Skill name must be 50 characters or less.';
    } else if (!/^[a-zA-Z0-9 .+#\-\/()]+$/.test(name)) {
      this.skillFormErrors['name'] = 'Only letters, numbers, spaces and . + # - / ( ) are allowed.';
    }

    if (this.selectedDomains.size === 0) {
      this.skillFormErrors['domains'] = 'Please select at least one domain.';
    }

    return Object.keys(this.skillFormErrors).length === 0;
  }

  addSkill() {
    if (!this.validateSkillForm()) return;

    const skillExists = this.skills.some(
      s => s.name.toLowerCase() === this.newSkillName.trim().toLowerCase()
    );
    if (skillExists) {
      this.skillFormErrors['name'] = `"${this.newSkillName.trim()}" is already in your profile.`;
      return;
    }

    const userId = this.auth.getUserId() || 1;
    const skill: Skill = {
      name: this.newSkillName.trim(),
      domains: [...this.selectedDomains],
      description: 'Added via Dashboard',
      userId
    };

    this.portfolioService.createSkill(skill).subscribe({
      next: () => {
        this.portfolioService.notifySkillsUpdated();
        this.showAddModal = false;
        this.newSkillName = '';
        this.selectedDomains = new Set();
        this.errorMessage = null;
        this.router.navigate(['/dashboard/my-portfolio']);
      },
      error: (err) => {
        console.error('Error adding skill:', err);
        if (err.error?.message?.includes('Duplicate entry')) {
          this.errorMessage = `The skill "${this.newSkillName}" already exists in your profile.`;
        } else {
          this.errorMessage = err.error?.message || err.message || 'Failed to add skill. Please try again.';
        }
        setTimeout(() => this.errorMessage = null, 5000);
      }
    });
  }

  // ── Delete ──────────────────────────────────────────────────

  confirmDelete(id: number) {
    this.skillToDeleteId = id;
    this.showDeleteConfirm = true;
  }

  cancelDelete() {
    this.showDeleteConfirm = false;
    this.skillToDeleteId = null;
  }

  deleteSkill() {
    if (this.skillToDeleteId) {
      const userId = this.auth.getUserId() || 1;
      this.portfolioService.deleteUserSkill(userId, this.skillToDeleteId).subscribe({
        next: () => {
          this.loadSkills();
          this.cancelDelete();
        },
        error: (err) => {
          console.error(err);
          this.errorMessage = 'Failed to delete skill.';
          this.cancelDelete();
        }
      });
    }
  }

  // ── Verify / Test ───────────────────────────────────────────

  verifySkill(skillId: number) {
    this.isGeneratingTest = true;
    this.showTestModal = true;
    this.currentTest = null;
    this.testResult = null;
    this.testAnswers = [];
    this.resetQuestionState();
    this.errorMessage = null;

    this.portfolioService.generateTest(skillId).subscribe({
      next: (test) => {
        this.isGeneratingTest = false;
        this.currentTest = test;
        this.currentQuestionIndex = 0;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isGeneratingTest = false;
        this.showTestModal = false;
        console.error('Error generating test:', err);

        if (err instanceof TimeoutError) {
          this.errorMessage = 'Request timed out after 2 minutes. Please try again.';
        } else if (err.status === 0) {
          this.errorMessage = 'Cannot connect to the Portfolio service (port 8086).';
        } else if (err.status === 404) {
          this.errorMessage = 'Skill not found. Please refresh the page.';
        } else if (err.status === 500) {
          this.errorMessage = 'AI service error. Check the API_KEY environment variable.';
        } else if (err.status === 504) {
          this.errorMessage = 'Gateway timeout. The AI service is taking too long.';
        } else {
          this.errorMessage = err.error?.message || 'Failed to generate test. Please try again later.';
        }
        setTimeout(() => this.errorMessage = null, 10000);
      }
    });
  }

  resetQuestionState() {
    this.selectedOption = '';
    this.isAnswerSubmitted = false;
    this.isAnswerCorrect = false;
  }

  submitAnswer() {
    if (!this.selectedOption || !this.currentTest) return;
    const q = this.currentTest.questions[this.currentQuestionIndex];
    this.selectedOption = this.selectedOption.substring(0, 8);
    this.isAnswerCorrect = this.selectedOption === q.correctOption;
    this.isAnswerSubmitted = true;
    this.testAnswers.push({ questionIndex: this.currentQuestionIndex, selectedOption: this.selectedOption });
  }

  nextQuestion() {
    if (this.currentTest && this.currentQuestionIndex < this.currentTest.questions.length - 1) {
      this.currentQuestionIndex++;
      this.resetQuestionState();
    } else {
      this.finishTest();
    }
  }

  finishTest() {
    if (!this.currentTest) return;
    const userId = this.auth.getUserId() || 1;
    this.portfolioService.submitTest({
      testId: this.currentTest.id,
      freelancerId: userId,
      answers: this.testAnswers
    }).subscribe({
      next: (result) => {
        this.testResult = result;
        this.loadSkills();
      },
      error: (err) => {
        console.error('Error submitting test:', err);
        this.errorMessage = 'Failed to submit test. Please try again.';
        setTimeout(() => this.errorMessage = null, 5000);
      }
    });
  }

  closeTestModal() {
    this.showTestModal = false;
    this.currentTest = null;
    this.testResult = null;
  }

  // ── History ─────────────────────────────────────────────────

  openHistoryModal() {
    this.showHistoryModal = true;
    this.isLoadingHistory = true;
    const userId = this.auth.getUserId() || 1;
    this.portfolioService.getUserEvaluations(userId).subscribe({
      next: (evaluations) => {
        this.evaluationHistory = evaluations.sort((a, b) =>
          new Date(b.evaluatedAt || '').getTime() - new Date(a.evaluatedAt || '').getTime()
        );
        this.isLoadingHistory = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading history:', err);
        this.isLoadingHistory = false;
        this.evaluationHistory = [];
        this.cdr.detectChanges();
      }
    });
  }

  closeHistoryModal() {
    this.showHistoryModal = false;
    this.evaluationHistory = [];
  }

  goToHistory() {
    this.closeTestModal();
    this.openHistoryModal();
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  getSkillColor(name: string): number {
    return ((name ? name.charCodeAt(0) : 0) % 6) + 1;
  }
}
