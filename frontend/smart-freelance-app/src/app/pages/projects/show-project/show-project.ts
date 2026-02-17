import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-show-project',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './show-project.html',
  styleUrl: './show-project.scss', 
})
export class ShowProject implements OnInit {

  project: any = null;
  isLoading = false;
  errorMessage: string | null = null;
  id!: number;

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadProject();
  }

  loadProject(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.projectService.getProjectById(this.id).subscribe({
      next: (res) => {
        this.project = res;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Failed to load project details.';
        this.isLoading = false;
      }
    });
  }

  getSkills(): string[] {
    if (!this.project?.skillsRequiered) return [];
    return this.project.skillsRequiered
      .split(',')
      .map((s: string) => s.trim())
      .filter((s: string) => s.length > 0);
  }
}
