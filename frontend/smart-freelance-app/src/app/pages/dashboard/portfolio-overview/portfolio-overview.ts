import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PortfolioService, Experience, Skill } from '../../../core/services/portfolio.service';
import { Card } from '../../../shared/components/card/card';
import { SkillManagement } from '../skill-management/skill-management';

@Component({
  selector: 'app-portfolio-overview',
  imports: [CommonModule, FormsModule, Card, SkillManagement],
  templateUrl: './portfolio-overview.html',
  styleUrl: './portfolio-overview.scss',
  standalone: true,
})
export class PortfolioOverview implements OnInit, OnDestroy {
  experiences: Experience[] = [];
  skills: Skill[] = [];

  // Form State
  showForm = false;
  isEditing = false;
  currentExperience!: Experience;

  // Helpers for form inputs
  newKeyTask = '';
  newSkillName = '';

  // Inline validation errors
  formErrors: Record<string, string> = {};

  get todayIso(): string {
    return new Date().toISOString().split('T')[0];
  }

  // Subscription management
  private skillsSubscription?: Subscription;

  constructor(
    public auth: AuthService,
    private portfolioService: PortfolioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.currentExperience = this.getEmptyExperience();
    console.log('PortfolioOverview initialized');

    // Load initial data
    this.loadExperiences();
    this.loadSkills();

    // Subscribe to skills updates (triggered when navigating back from skill management)
    this.skillsSubscription = this.portfolioService.skillsUpdated$.subscribe(() => {
      console.log('Skills updated notification received, refreshing skills...');
      // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
      setTimeout(() => {
        this.loadSkills();
      }, 0);
    });
  }

  ngOnDestroy() {
    // Clean up subscription to prevent memory leaks
    this.skillsSubscription?.unsubscribe();
  }

  getEmptyExperience(): Experience {
    return {
      userId: this.auth.getUserId() || 1,
      title: '',
      type: 'JOB',
      description: '',
      startDate: '',
      endDate: '',
      companyOrClientName: '',
      keyTasks: [],
      skillNames: []
    };
  }

  loadExperiences() {
    const userId = this.auth.getUserId() || 1;
    console.log('Loading experiences for user:', userId);
    this.portfolioService.getExperiences(userId).subscribe({
      next: (exps) => {
        console.log('Experiences loaded:', exps);
        this.experiences = exps;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading experiences:', err);
      }
    });
  }

  loadSkills() {
    const userId = this.auth.getUserId() || 1;
    console.log('Loading skills for user:', userId);
    this.portfolioService.getUserSkills(userId).subscribe({
      next: (skills) => {
        console.log('Skills loaded in portfolio:', skills);
        this.skills = skills;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading skills:', err);
      }
    });
  }

  openAddForm() {
    this.currentExperience = this.getEmptyExperience();
    this.isEditing = false;
    this.formErrors = {};
    this.showForm = true;
  }

  openEditForm(exp: Experience) {
    this.currentExperience = { ...exp, skillNames: [] };
    if(exp.skills) {
      this.currentExperience.skillNames = exp.skills.map(s => s.name);
    }
    this.isEditing = true;
    this.formErrors = {};
    this.showForm = true;
  }

  deleteExperience(id: number) {
    if(confirm('Delete this experience?')) {
      this.portfolioService.deleteExperience(id).subscribe(() => this.loadExperiences());
    }
  }

  private isValidDate(value: string): boolean {
    if (!value) return true;
    const iso = /^\d{4}-\d{2}-\d{2}$/.test(value);
    if (!iso) return false;
    const d = new Date(value);
    return !isNaN(d.getTime());
  }

  private validateExperienceForm(): boolean {
    this.formErrors = {};
    const e = this.currentExperience;

    if (!e.title?.trim()) {
      this.formErrors['title'] = 'Job title / role is required.';
    } else if (e.title.trim().length < 3) {
      this.formErrors['title'] = 'Title must be at least 3 characters.';
    } else if (e.title.trim().length > 100) {
      this.formErrors['title'] = 'Title must be 100 characters or less.';
    }

    if (!e.companyOrClientName?.trim()) {
      this.formErrors['company'] = 'Company / client name is required.';
    } else if (e.companyOrClientName.trim().length < 2) {
      this.formErrors['company'] = 'Must be at least 2 characters.';
    } else if (e.companyOrClientName.trim().length > 100) {
      this.formErrors['company'] = 'Must be 100 characters or less.';
    }

    if (!e.startDate) {
      this.formErrors['startDate'] = 'Start date is required.';
    } else if (!this.isValidDate(e.startDate)) {
      this.formErrors['startDate'] = 'Please enter a valid date.';
    } else if (new Date(e.startDate) > new Date()) {
      this.formErrors['startDate'] = 'Start date cannot be in the future.';
    }

    if (e.endDate) {
      if (!this.isValidDate(e.endDate)) {
        this.formErrors['endDate'] = 'Please enter a valid date.';
      } else if (e.startDate && new Date(e.endDate) < new Date(e.startDate)) {
        this.formErrors['endDate'] = 'End date must be after start date.';
      }
    }

    if (!e.description?.trim()) {
      this.formErrors['description'] = 'Description is required.';
    } else if (e.description.trim().length < 20) {
      this.formErrors['description'] = `At least 20 characters required (${e.description.trim().length}/20).`;
    }

    return Object.keys(this.formErrors).length === 0;
  }

  saveExperience() {
    if (!this.validateExperienceForm()) return;

    if (this.isEditing && this.currentExperience.id) {
      this.portfolioService.updateExperience(this.currentExperience.id, this.currentExperience).subscribe(() => {
        this.loadExperiences();
        this.portfolioService.notifySkillsUpdated();
        this.showForm = false;
      });
    } else {
      this.portfolioService.createExperience(this.currentExperience).subscribe(() => {
        this.loadExperiences();
        this.portfolioService.notifySkillsUpdated();
        this.showForm = false;
      });
    }
  }


  addKeyTask() {
    if (this.newKeyTask) {
      this.currentExperience.keyTasks.push(this.newKeyTask);
      this.newKeyTask = '';
    }
  }

  removeKeyTask(index: number) {
    this.currentExperience.keyTasks.splice(index, 1);
  }

  addSkillTag() {
    if (this.newSkillName) {
      if (!this.currentExperience.skillNames) this.currentExperience.skillNames = [];
      this.currentExperience.skillNames.push(this.newSkillName);
      this.newSkillName = '';
    }
  }

  removeSkillTag(index: number) {
    this.currentExperience.skillNames?.splice(index, 1);
  }
}
