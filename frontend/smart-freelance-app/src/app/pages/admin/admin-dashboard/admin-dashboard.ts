import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { Card } from '../../../shared/components/card/card';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { OfferService } from '../../../core/services/offer.service';
import { ReviewService, ReviewStats } from '../../../core/services/review.service';
import { TaskService, TaskStatsDto } from '../../../core/services/task.service';
import { PlanningService, DashboardStatsDto, FreelancerActivityDto, ProjectActivityDto } from '../../../core/services/planning.service';
import { ContractService } from '../../../core/services/contract.service';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, Card, BaseChartDirective],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard implements OnInit {
  loading = true;
  users: User[] = [];
  projects: Project[] = [];
  offersTotal = 0;
  reviewStats: ReviewStats | null = null;
  taskStats: TaskStatsDto | null = null;
  planningStats: DashboardStatsDto | null = null;
  contractsTotal = 0;
  topFreelancers: FreelancerActivityDto[] = [];
  mostActiveProjects: ProjectActivityDto[] = [];
  projectIdToTitle: Record<number, string> = {};
  freelancerIdToName: Record<number, string> = {};

  // Charts
  userRolesChartData: ChartData<'pie'> = {
    labels: ['Clients', 'Freelancers', 'Admins'],
    datasets: [{ data: [0, 0, 0], backgroundColor: ['#E37E33', '#3B82F6', '#10B981'], hoverOffset: 4 }],
  };
  userRolesChartOptions: ChartOptions<'pie'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
  };

  taskStatusChartData: ChartData<'doughnut'> = {
    labels: ['Done', 'In Progress', 'Todo', 'Overdue'],
    datasets: [{
      data: [0, 0, 0, 0],
      backgroundColor: ['#10B981', '#3B82F6', '#6B7280', '#EF4444'],
      hoverOffset: 4,
    }],
  };
  taskStatusChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
  };

  reviewRatingsChartData: ChartData<'bar'> = {
    labels: ['1★', '2★', '3★', '4★', '5★'],
    datasets: [{ data: [0, 0, 0, 0, 0], label: 'Reviews', backgroundColor: '#E37E33' }],
  };
  reviewRatingsChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: {
      x: { beginAtZero: true, ticks: { stepSize: 1 } },
      y: { ticks: { font: { size: 12 } } },
    },
  };

  activityChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{ data: [], label: 'Updates', backgroundColor: '#E37E33', borderRadius: 4 }],
  };
  activityChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { beginAtZero: true, ticks: { stepSize: 1 } },
    },
  };

  constructor(
    private userService: UserService,
    private projectService: ProjectService,
    private offerService: OfferService,
    private reviewService: ReviewService,
    private taskService: TaskService,
    private planningService: PlanningService,
    private contractService: ContractService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.loading = true;
    forkJoin({
      users: this.userService.getAll().pipe(catchError(() => of([]))),
      projects: this.projectService.getAllProjects().pipe(catchError(() => of([]))),
      offers: this.offerService.searchOffers({ page: 0, size: 1 }).pipe(
        catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size: 1, number: 0, first: true, last: true }))
      ),
      reviewStats: this.reviewService.getStats().pipe(catchError(() => of(null))),
      taskStats: this.taskService.getStatsDashboard().pipe(catchError(() => of(null))),
      planningStats: this.planningService.getDashboardStats().pipe(catchError(() => of(null))),
      contracts: this.contractService.getAll().pipe(catchError(() => of([]))),
      topFreelancers: this.planningService.getFreelancersByActivity(8).pipe(catchError(() => of([]))),
      mostActiveProjects: this.planningService.getMostActiveProjects(8).pipe(catchError(() => of([]))),
    })
      .pipe(finalize(() => { this.loading = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (res) => {
          this.users = res.users;
          this.projects = res.projects;
          this.offersTotal = res.offers?.totalElements ?? 0;
          this.reviewStats = res.reviewStats ?? null;
          this.taskStats = res.taskStats ?? null;
          this.planningStats = res.planningStats ?? null;
          this.contractsTotal = Array.isArray(res.contracts) ? res.contracts.length : 0;
          this.topFreelancers = res.topFreelancers ?? [];
          this.mostActiveProjects = res.mostActiveProjects ?? [];

          this.buildProjectTitles();
          this.buildFreelancerNames();
          this.updateCharts();
          this.cdr.detectChanges();
        },
      });
  }

  private buildProjectTitles(): void {
    this.projectIdToTitle = {};
    for (const p of this.projects) {
      if (p.id != null) this.projectIdToTitle[p.id] = p.title || `Project #${p.id}`;
    }
  }

  private buildFreelancerNames(): void {
    this.freelancerIdToName = {};
    for (const u of this.users) {
      if (u.role === 'FREELANCER' && u.id) {
        this.freelancerIdToName[u.id] = `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.email || `User #${u.id}`;
      }
    }
  }

  private updateCharts(): void {
    // User roles
    const clients = this.users.filter((u) => u.role === 'CLIENT').length;
    const freelancers = this.users.filter((u) => u.role === 'FREELANCER').length;
    const admins = this.users.filter((u) => u.role === 'ADMIN').length;
    this.userRolesChartData = {
      labels: ['Clients', 'Freelancers', 'Admins'],
      datasets: [{
        data: [clients, freelancers, admins],
        backgroundColor: ['#E37E33', '#3B82F6', '#10B981'],
        hoverOffset: 4,
      }],
    };

    // Task status
    const taskStats = this.taskStats;
    const done = taskStats?.doneCount ?? 0;
    const inProgress = taskStats?.inProgressCount ?? 0;
    const overdue = taskStats?.overdueCount ?? 0;
    const total = taskStats?.totalTasks ?? 0;
    const todo = Math.max(0, total - done - inProgress - overdue);
    this.taskStatusChartData = {
      labels: ['Done', 'In Progress', 'Todo', 'Overdue'],
      datasets: [{
        data: [done, inProgress, todo, overdue],
        backgroundColor: ['#10B981', '#3B82F6', '#6B7280', '#EF4444'],
        hoverOffset: 4,
      }],
    };

    // Review ratings
    const rs = this.reviewStats?.countByRating ?? {};
    this.reviewRatingsChartData = {
      labels: ['1★', '2★', '3★', '4★', '5★'],
      datasets: [{
        data: [rs[1] ?? 0, rs[2] ?? 0, rs[3] ?? 0, rs[4] ?? 0, rs[5] ?? 0],
        label: 'Reviews',
        backgroundColor: '#E37E33',
      }],
    };

    // Top freelancers activity (horizontal bar)
    const labels = this.topFreelancers.map((f) => this.freelancerIdToName[f.freelancerId] || `#${f.freelancerId}`);
    const data = this.topFreelancers.map((f) => f.updateCount);
    this.activityChartData = {
      labels,
      datasets: [{ data, label: 'Updates', backgroundColor: '#E37E33', borderRadius: 4 }],
    };
  }

  getProjectTitle(id: number): string {
    return this.projectIdToTitle[id] ?? `Project #${id}`;
  }

  getFreelancerName(id: number): string {
    return this.freelancerIdToName[id] ?? `User #${id}`;
  }

  formatPercent(val: number | null | undefined): string {
    return val != null ? `${val.toFixed(1)}%` : '—';
  }
}
