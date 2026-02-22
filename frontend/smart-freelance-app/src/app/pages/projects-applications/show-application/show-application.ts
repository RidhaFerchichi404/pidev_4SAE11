import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ProjectApplication, ProjectApplicationService } from '../../../core/services/project-application.service';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../core/services/user.service';
import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-show-application',
  imports: [CommonModule, RouterModule],
  templateUrl: './show-application.html',
  styleUrl: './show-application.scss',
})
export class ShowApplication implements OnInit{
  application: ProjectApplication | null = null;
  user: any = null;
  isLoading = true;
  errorMessage: string | null = null;
  id!: number;

  constructor(
    private route: ActivatedRoute,
    private applicationService: ProjectApplicationService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));

    if (!this.id || Number.isNaN(this.id)) {
      this.errorMessage = 'Invalid application.';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.loadApplication();
  }

  loadApplication(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    this.applicationService.getApplicationById(this.id).subscribe({
      next: (res: ProjectApplication | null) => {
        if (!res) {
          this.errorMessage = 'Application not found.';
          this.isLoading = false;
          this.cdr.detectChanges();
          return;
        }

        this.application = res;

        // Load freelancer info
        this.userService.getById(res.freelanceId).subscribe({
          next: (user) => {
            this.user = user;
            this.isLoading = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.user = null;
            this.isLoading = false;
            this.cdr.detectChanges();
          },
        });
      },
      error: () => {
        this.errorMessage = 'Failed to load application.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }
}
