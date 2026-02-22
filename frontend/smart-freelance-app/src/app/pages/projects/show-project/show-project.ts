import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProjectService, Project } from '../../../core/services/project.service';
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
  changeApplicationStatus(application: ProjectApplication, status: 'ACCEPTED' | 'REJECTED'): void {
    if (!application.id) return;
    
    this.applicationService.updateApplication({ id: application.id, status })
    .subscribe({
      next: (updated) => {
        if (updated) {
          // Update locally so the UI reflects the change immediately
          application.status = status;
          this.cdr.detectChanges();
        }
      },
      error: () => {
        console.error('Failed to update application status');
      }
    });
  }

  getSkills(): string[] {
    const skills = this.project?.skillsRequiered;
    if (!skills) return [];
    const str = Array.isArray(skills) ? (skills as string[]).join(',') : String(skills);
    return str
      .split(',')
      .map((s: string) => s.trim())
      .filter((s: string) => s.length > 0);
  }
}
