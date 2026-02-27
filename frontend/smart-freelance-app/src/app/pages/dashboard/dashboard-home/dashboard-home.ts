import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { Card } from '../../../shared/components/card/card';
import { Project, ProjectService } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard-home',
  imports: [Card, RouterModule, CommonModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
  standalone: true,
})
export class DashboardHome implements OnInit{
  constructor(
    public auth: AuthService,
    private ps: ProjectService,
    private us: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  userRole: string | null = null;
  recommendedProjects: Project[] = [];
  isLoadingRecommendations = false;

  ngOnInit(): void {

    this.userRole = this.auth.getUserRole();
    if (this.userRole === 'FREELANCER') {

      const email = this.auth.getPreferredUsername();

      if (!email) return; // safety check

      this.us.getByEmail(email).subscribe(user => {
        if (user?.id) {
          this.loadRecommendations(user.id);
        }
      });

    }
  }


  loadRecommendations(userId: number): void {

    this.isLoadingRecommendations = true;
    this.cdr.detectChanges(); // 👈 force UI update

    this.ps.getRecommendedProjects(userId)
      .subscribe({
        next: (projects) => {

          this.recommendedProjects = projects || [];
          this.isLoadingRecommendations = false;

          this.cdr.detectChanges(); // 👈 update UI
        },
        error: () => {

          this.isLoadingRecommendations = false;
          this.cdr.detectChanges(); // 👈 update UI
        }
      });
  }

}
