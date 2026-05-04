import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProjectService, Project, ProjectRisk, ProjectSatisfaction } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectApplication, ProjectApplicationService } from '../../../core/services/project-application.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-show-project',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './show-project.html',
  styleUrl: './show-project.scss',
})
export class ShowProject implements OnInit {
  project: Project | null = null;
  applications: ProjectApplication[] = [];
  usersMap: { [key: number]: any } = {};
  isLoading = true;
  isLoadingApplications = false;
  errorMessage: string | null = null;
  id!: number;

  public userRole: string | null = null;
  projectRisk: ProjectRisk | null = null;
  riskLoading = false;
  projectSatisfaction: ProjectSatisfaction | null = null;
  satisfactionLoading = false;
  showRiskReasons = false;
  showSatisfactionReasons = false;

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private applicationService: ProjectApplicationService,
    private cdr: ChangeDetectorRef,
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.userRole = this.authService.getUserRole();

    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.id || Number.isNaN(this.id)) {
      this.errorMessage = 'Invalid project.';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }
    this.loadProject();
  }

  loadProject(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    this.projectService.getProjectById(this.id).subscribe({
      next: (res: Project | null) => {
        this.project = res ?? null;
        if (!this.project) {
          this.errorMessage = 'Project not found.';
        } else {
          this.loadProjectRisk();
          this.loadProjectSatisfaction();
        }

        // Load applications only if the user is a client
        if (this.userRole === 'CLIENT' || this.userRole === 'ADMIN') {
          this.loadApplications();
        }

        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load project details.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadProjectRisk(): void {
    this.riskLoading = true;
    this.projectService.getProjectRisk(this.id).subscribe({
      next: (r) => {
        this.projectRisk = r;
        this.showRiskReasons = false;
        this.riskLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.projectRisk = null;
        this.riskLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadProjectSatisfaction(): void {
    this.satisfactionLoading = true;
    this.projectService.getProjectSatisfaction(this.id).subscribe({
      next: (res) => {
        this.projectSatisfaction = res;
        this.showSatisfactionReasons = false;
        this.satisfactionLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.projectSatisfaction = null;
        this.satisfactionLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get riskPercent(): number {
    return Math.max(0, Math.min(100, this.projectRisk?.riskPercent ?? 0));
  }

  /** Success likelihood (%) aligned with model output (complements failure risk). */
  get successLikelihoodPercent(): number {
    const p = this.projectRisk?.successProbability;
    if (p == null || Number.isNaN(p)) return 0;
    return Math.round(Math.max(0, Math.min(1, p)) * 1000) / 10;
  }

  /** Remaining bar width so failure + success segments fill 100%. */
  get successBarPercent(): number {
    return Math.max(0, Math.min(100, 100 - this.riskPercent));
  }

  get riskReasons(): string[] {
    const r = this.projectRisk?.reasons;
    return Array.isArray(r) ? r.filter((s) => !!s?.trim()) : [];
  }

  get visibleRiskReasons(): string[] {
    return this.showRiskReasons ? this.riskReasons : this.riskReasons.slice(0, 2);
  }

  get hasMoreRiskReasons(): boolean {
    return this.riskReasons.length > 2;
  }

  get satisfactionScore(): number {
    return Math.max(0, Math.min(10, this.projectSatisfaction?.satisfactionScore ?? 0));
  }

  get satisfactionPercent(): number {
    return Math.max(0, Math.min(100, this.projectSatisfaction?.satisfactionPercent ?? 0));
  }

  get satisfactionReasons(): string[] {
    const r = this.projectSatisfaction?.reasons;
    return Array.isArray(r) ? r.filter((s) => !!s?.trim()) : [];
  }

  get visibleSatisfactionReasons(): string[] {
    return this.showSatisfactionReasons ? this.satisfactionReasons : this.satisfactionReasons.slice(0, 2);
  }

  get hasMoreSatisfactionReasons(): boolean {
    return this.satisfactionReasons.length > 2;
  }

  get satisfactionTierLabel(): string {
    if (this.satisfactionScore >= 8) return 'Excellent';
    if (this.satisfactionScore >= 6.5) return 'Good';
    if (this.satisfactionScore >= 5) return 'Average';
    return 'Needs attention';
  }

  get satisfactionToneClass(): string {
    if (this.satisfactionScore >= 8) return 'excellent';
    if (this.satisfactionScore >= 6.5) return 'good';
    if (this.satisfactionScore >= 5) return 'average';
    return 'poor';
  }

  get riskTierLabel(): string {
    if (this.riskPercent >= 70) return 'High risk';
    if (this.riskPercent >= 40) return 'Moderate risk';
    return 'Low risk';
  }

  get riskToneClass(): string {
    if (this.riskPercent >= 70) return 'high';
    if (this.riskPercent >= 40) return 'medium';
    return 'low';
  }

  loadApplications(): void {
    if (!this.project?.id) return;

    this.isLoadingApplications = true;
    this.applicationService.getApplicationsByProject(this.project.id).subscribe({
      next: (res) => {
        // Optional: fetch freelancer info for each application
        this.applications = res;

        // For each application, fetch freelancer
        this.applications.forEach(app => {
          this.userService.getById(app.freelanceId).subscribe({
            next: (user) => {
              this.usersMap[app.freelanceId] = user;
              this.cdr.detectChanges();
            },
            error: () => {
              this.usersMap[app.freelanceId] = null;
            }
          });
        });

        this.isLoadingApplications = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingApplications = false;
        this.cdr.detectChanges();
      }
    });
  }

  // Accept or reject an application
  changeApplicationStatus(
    application: ProjectApplication,
    status: 'ACCEPTED' | 'REJECTED'
  ): void {

    if (!application.id) return;

    this.applicationService
      .updateStatus(application.id, status)
      .subscribe({
        next: (updated) => {
          if (updated) {
            // Update UI immediately
            application.status = updated.status;
            application.respondedAt = updated.respondedAt;

            this.cdr.detectChanges();
          }
        },
        error: () => {
          console.error('Failed to update application status');
        }
      });
  }

  getSkills(): string[] {
    return this.project?.skills?.map(skill => skill.name) ?? [];
  }

  toggleRiskReasons(): void {
    this.showRiskReasons = !this.showRiskReasons;
  }

  toggleSatisfactionReasons(): void {
    this.showSatisfactionReasons = !this.showSatisfactionReasons;
  }
}
