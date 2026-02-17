import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-list-projects',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './list-projects.html',
  styleUrl: './list-projects.scss',
})
export class ListProjects implements OnInit{
  projects: any = [];
  isLoading = false;
  errorMessage: string | null = null;

  constructor(private projectService: ProjectService) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.projectService.getAllProjects().subscribe({
      next: (res) => {
        this.projects = res;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load projects.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  deleteProject(id?: number): void {
    if (!id) return;

    if (!confirm('Are you sure you want to delete this project?')) return;

    this.projectService.deleteProject(id).subscribe({
      next: () => {
        this.projects = this.projects.filter((p: any) => p.id !== id);
      },
      error: (err) => {
        console.error('Delete failed', err);
        alert('Failed to delete project');
      }
    });
  }
}
