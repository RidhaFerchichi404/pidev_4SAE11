import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ProjectApplication, ProjectApplicationService } from '../../../core/services/project-application.service';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-list-application',
  imports: [RouterModule, CommonModule],
  templateUrl: './list-application.html',
  styleUrl: './list-application.scss',
})
export class ListApplication implements OnInit {
  applications: ProjectApplication[] = [];

  isLoading = false;
  errorMessage: string | null = null;

  applicationToDelete: ProjectApplication | null = null;
  deleting = false;

  constructor(
    private applicationService: ProjectApplicationService,
    private userService: UserService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    const email = this.authService.getPreferredUsername();
    if (!email) {
      this.errorMessage = 'You must be signed in.';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.userService.getByEmail(email).subscribe({
      next: (user) => {
        if (!user?.id) {
          this.errorMessage = 'Could not identify your account.';
          this.isLoading = false;
          this.cdr.detectChanges();
          return;
        }

        this.applicationService.getApplicationsByFreelance(user.id).subscribe({
          next: (data) => {
            this.applications = data || [];
            this.isLoading = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.errorMessage = 'Failed to load applications.';
            this.isLoading = false;
            this.cdr.detectChanges();
          },
        });
      },
      error: () => {
        this.errorMessage = 'Failed to load user profile.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(application: ProjectApplication): void {
    this.applicationToDelete = application;
  }

  closeDeleteModal(): void {
    this.applicationToDelete = null;
  }

  doDelete(): void {
    if (!this.applicationToDelete?.id) return;

    this.deleting = true;

    this.applicationService.deleteApplication(this.applicationToDelete.id).subscribe({
      next: (success) => {
        if (success) {
          this.applications = this.applications.filter(
            (app) => app.id !== this.applicationToDelete?.id,
          );
        }
        this.deleting = false;
        this.closeDeleteModal();
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.closeDeleteModal();
        this.cdr.detectChanges();
      },
    });
  }
}
