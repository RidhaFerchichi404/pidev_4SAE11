import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortfolioService, SkillUsageStat, SkillSuccessStat } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-skill-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skill-stats.html',
  styleUrl: './skill-stats.scss',
})
export class SkillStats implements OnInit {
  usageStats: SkillUsageStat[] = [];
  successStats: SkillSuccessStat[] = [];

  isLoadingUsage = false;
  isLoadingSuccess = false;

  usageError: string | null = null;
  successError: string | null = null;

  constructor(private portfolioService: PortfolioService) {}

  ngOnInit() {
    this.loadUsage();
    this.loadSuccess();
  }

  private loadUsage() {
    this.isLoadingUsage = true;
    this.portfolioService.getSkillUsageStats().subscribe({
      next: (data) => {
        this.usageStats = data;
        this.isLoadingUsage = false;
      },
      error: () => {
        this.usageError = 'Could not load skill usage data.';
        this.isLoadingUsage = false;
      }
    });
  }

  private loadSuccess() {
    this.isLoadingSuccess = true;
    this.portfolioService.getSkillSuccessStats().subscribe({
      next: (data) => {
        this.successStats = data;
        this.isLoadingSuccess = false;
      },
      error: () => {
        this.successError = 'Could not load skill success data.';
        this.isLoadingSuccess = false;
      }
    });
  }

  getSuccessBarColor(rate: number): string {
    if (rate >= 70) return '#10B981';
    if (rate >= 40) return '#F59E0B';
    return '#EF4444';
  }
}
