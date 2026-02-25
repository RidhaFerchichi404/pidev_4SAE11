import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, TimeoutError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PortfolioService, Skill, EvaluationTest, EvaluationResult } from '../../../core/services/portfolio.service';
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
  newSkillName = '';
  newSkillDomain = '';

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

    // Subscribe to skills updates from other components
    this.skillsSubscription = this.portfolioService.skillsUpdated$.subscribe(() => {
      setTimeout(() => {
        this.loadSkills();
      }, 0);
    });
  }

  ngOnDestroy() {
    this.skillsSubscription?.unsubscribe();
  }

  loadSkills() {
    const userId = this.auth.getUserId() || 1;
    console.log('Loading skills for userId:', userId);
    this.portfolioService.getUserSkills(userId).subscribe({
      next: (skills) => {
        console.log('Skills loaded:', skills);
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

  get groupedSkills(): { [domain: string]: Skill[] } {
    return this.skills.reduce((acc, skill) => {
      const domain = skill.domain || 'Other';
      if (!acc[domain]) {
        acc[domain] = [];
      }
      acc[domain].push(skill);
      return acc;
    }, {} as { [domain: string]: Skill[] });
  }

  get skillDomains(): string[] {
    return Object.keys(this.groupedSkills).sort();
  }

  getSkillsByDomain(domain: string): Skill[] {
    return this.groupedSkills[domain] || [];
  }

  openAddModal() {
    this.errorMessage = null;
    this.newSkillName = '';
    this.newSkillDomain = '';
    this.skillFormErrors = {};
    this.showAddModal = true;
  }

  private validateSkillForm(): boolean {
    this.skillFormErrors = {};
    const name = this.newSkillName.trim();
    const domain = this.newSkillDomain.trim();

    if (!name) {
      this.skillFormErrors['name'] = 'Skill name is required.';
    } else if (name.length < 2) {
      this.skillFormErrors['name'] = 'Skill name must be at least 2 characters.';
    } else if (name.length > 50) {
      this.skillFormErrors['name'] = 'Skill name must be 50 characters or less.';
    } else if (!/^[a-zA-Z0-9 .+#\-\/()]+$/.test(name)) {
      this.skillFormErrors['name'] = 'Only letters, numbers, spaces and . + # - / ( ) are allowed.';
    }

    if (!domain) {
      this.skillFormErrors['domain'] = 'Domain is required.';
    } else if (domain.length < 2) {
      this.skillFormErrors['domain'] = 'Domain must be at least 2 characters.';
    } else if (domain.length > 30) {
      this.skillFormErrors['domain'] = 'Domain must be 30 characters or less.';
    }

    return Object.keys(this.skillFormErrors).length === 0;
  }

  addSkill() {
    if (!this.validateSkillForm()) return;

    const skillExists = this.skills.some(
      skill => skill.name.toLowerCase() === this.newSkillName.trim().toLowerCase()
    );

    if (skillExists) {
      this.skillFormErrors['name'] = `"${this.newSkillName.trim()}" is already in your profile.`;
      return;
    }

    const userId = this.auth.getUserId() || 1;
    console.log('Creating skill with userId:', userId);
    const skill = {
      name: this.newSkillName,
      domain: this.newSkillDomain,
      description: 'Added via Dashboard',
      userId: userId
    };
    console.log('Skill to create:', skill);
    this.portfolioService.createSkill(skill).subscribe({
      next: (createdSkill) => {
        console.log('Skill created successfully:', createdSkill);
        this.portfolioService.notifySkillsUpdated();
        this.showAddModal = false;
        this.newSkillName = '';
        this.newSkillDomain = '';
        this.errorMessage = null;
        this.router.navigate(['/dashboard/my-portfolio']);
      },
      error: (err) => {
        console.error('Error adding skill:', err);
        if (err.error && err.error.message && err.error.message.includes('Duplicate entry')) {
          this.errorMessage = `The skill "${this.newSkillName}" already exists in your profile. Please add a different skill.`;
        } else if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else if (err.message) {
          this.errorMessage = err.message;
        } else {
          this.errorMessage = 'Failed to add skill. Please try again.';
        }
        setTimeout(() => this.errorMessage = null, 5000);
      }
    });
  }

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

  verifySkill(skillId: number) {
    // Show modal with loading spinner immediately
    this.isGeneratingTest = true;
    this.showTestModal = true;
    this.currentTest = null;
    this.testResult = null;
    this.testAnswers = [];
    this.resetQuestionState();
    this.errorMessage = null;
    console.log('Generating test for skill ID:', skillId);

    this.portfolioService.generateTest(skillId).subscribe({
      next: (test) => {
        console.log('Test generated successfully:', test);
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
          this.errorMessage = 'Request timed out after 2 minutes. The AI service may be taking too long to respond. Please try again.';
        } else if (err.status === 0) {
          this.errorMessage = 'Cannot connect to the Portfolio service. Please ensure the service is running on port 8086.';
        } else if (err.status === 404) {
          this.errorMessage = 'Skill not found. Please try refreshing the page.';
        } else if (err.status === 500) {
          this.errorMessage = 'AI service error. Please check that the API_KEY environment variable is set correctly.';
        } else if (err.status === 504 || err.statusText === 'Gateway Timeout') {
          this.errorMessage = 'Gateway timeout. The AI service may be taking too long to respond.';
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

    const currentQuestion = this.currentTest.questions[this.currentQuestionIndex];
    this.selectedOption = this.selectedOption.substring(0, 8);
    this.isAnswerCorrect = this.selectedOption === currentQuestion.correctOption;
    this.isAnswerSubmitted = true;

    this.testAnswers.push({
      questionIndex: this.currentQuestionIndex,
      selectedOption: this.selectedOption
    });
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
        // Reload skills so verified/score updates appear
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

  // ── History ────────────────────────────────
  openHistoryModal() {
    this.showHistoryModal = true;
    this.isLoadingHistory = true;
    const userId = this.auth.getUserId() || 1;
    this.portfolioService.getUserEvaluations(userId).subscribe({
      next: (evaluations) => {
        // Sort newest first
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

  /** Close the test result modal and immediately open the history modal */
  goToHistory() {
    this.closeTestModal();
    this.openHistoryModal();
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  /** Returns 1-6 deterministically from the skill name for color theming */
  getSkillColor(name: string): number {
    const code = name ? name.charCodeAt(0) : 0;
    return (code % 6) + 1;
  }
}
