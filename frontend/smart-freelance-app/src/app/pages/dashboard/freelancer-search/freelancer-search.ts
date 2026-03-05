import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  FreelancerService,
  FreelancerCard,
} from '../../../core/services/freelancer.service';
import { AuthService } from '../../../core/services/auth.service';

const SKILL_FILTERS = [
  'All', 'Angular', 'React', 'Spring Boot', 'Java', 'Python',
  'DevOps', 'UI/UX Design', 'Mobile', 'Data Science',
];

@Component({
  selector: 'app-freelancer-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './freelancer-search.html',
  styleUrl: './freelancer-search.scss',
})
export class FreelancerSearch implements OnInit {
  allFreelancers: FreelancerCard[] = [];
  filtered: FreelancerCard[] = [];
  isLoading = true;

  search = '';
  activeSkill = 'All';
  sortBy: 'rating' | 'reviews' | 'rate' = 'rating';

  readonly skillFilters = SKILL_FILTERS;

  constructor(
    public auth: AuthService,
    private freelancerSvc: FreelancerService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.freelancerSvc.getAllFreelancers().subscribe({
      next: cards => {
        this.allFreelancers = cards;
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.allFreelancers = [];
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  applyFilters(): void {
    const q = this.search.toLowerCase().trim();
    let list = this.allFreelancers.filter(f => {
      const matchSearch = !q
        || `${f.firstName} ${f.lastName}`.toLowerCase().includes(q)
        || f.title.toLowerCase().includes(q)
        || f.skills.some(s => s.name.toLowerCase().includes(q));
      const matchSkill = this.activeSkill === 'All'
        || f.skills.some(s => s.name.toLowerCase().includes(this.activeSkill.toLowerCase()));
      return matchSearch && matchSkill;
    });

    list = [...list].sort((a, b) =>
      this.sortBy === 'rating' ? b.rating - a.rating :
      this.sortBy === 'reviews' ? b.totalReviews - a.totalReviews :
      0
    );
    this.filtered = list;
  }

  setSkill(skill: string): void {
    this.activeSkill = skill;
    this.applyFilters();
  }

  viewPortfolio(f: FreelancerCard): void {
    this.router.navigate(['/dashboard/freelancer-portfolio', f.userId]);
  }

  initials(f: FreelancerCard): string {
    return (f.firstName[0] + f.lastName[0]).toUpperCase();
  }

  avatarGradient(uid: number): string {
    const hue = (uid * 137) % 360;
    return `linear-gradient(135deg, hsl(${hue}, 60%, 50%), hsl(${hue}, 60%, 40%))`;
  }

  stars(rating: number): number[] {
    return [1, 2, 3, 4, 5];
  }

  isFilled(star: number, rating: number): boolean {
    return star <= Math.round(rating);
  }
}
