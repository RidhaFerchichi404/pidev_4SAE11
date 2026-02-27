import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProjectService, Project } from '../../../core/services/project.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-project-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './project-management.html',
  styleUrl: './project-management.scss',
})
export class ProjectManagement implements OnInit {
  projects: Project[] = [];
  loading = true;
  errorMessage = '';
  projectToDelete: Project | null = null;
  deleting = false;
  addForm: FormGroup;
  editForm: FormGroup;
  addModalOpen = false;
  editingProject: Project | null = null;
  saving = false;
  adding = false;

  constructor(
    private projectService: ProjectService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      clientId: [null as number | null, [Validators.required, Validators.min(1)]],
      freelancerId: [null as number | null],
      title: ['', Validators.required],
      description: ['', Validators.required],
      budget: [null as number | null, Validators.min(0)],
      deadline: [''],
      status: [''],
      category: [''],
    });
    this.editForm = this.fb.group({
      clientId: [null as number | null, [Validators.required, Validators.min(1)]],
      freelancerId: [null as number | null],
      title: ['', Validators.required],
      description: ['', Validators.required],
      budget: [null as number | null, Validators.min(0)],
      deadline: [''],
      status: [''],
      category: [''],
    });
  }

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.errorMessage = '';
    this.projectService.getAllProjects().subscribe({
      next: (list) => {
        this.projects = list ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load projects.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openAdd(): void {
    this.errorMessage = '';
    this.addForm.reset({
      clientId: null,
      freelancerId: null,
      title: '',
      description: '',
      budget: null,
      deadline: '',
      status: '',
      category: '',
    });
    this.addModalOpen = true;
  }

  closeAdd(): void {
    if (!this.adding) this.addModalOpen = false;
  }

  saveAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    const payload: Partial<Project> = {
      clientId: v.clientId != null ? Number(v.clientId) : undefined,
      title: v.title.trim(),
      description: v.description.trim(),
      budget: v.budget != null && v.budget !== '' ? Number(v.budget) : undefined,
      deadline: v.deadline?.trim() || undefined,
      status: v.status?.trim() || undefined,
      category: v.category?.trim() || undefined,
    };
    this.adding = true;
    this.projectService.createProject(payload).subscribe({
      next: (created) => {
        this.adding = false;
        if (created) {
          this.addModalOpen = false;
          this.loadProjects();
        } else this.errorMessage = 'Failed to create project.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.adding = false;
        this.errorMessage = 'Failed to create project.';
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(project: Project): void {
    this.editingProject = project;
    this.projectToDelete = null;
    this.editForm.patchValue({
      clientId: project.clientId ?? null,
      title: project.title,
      description: project.description,
      budget: project.budget ?? null,
      deadline: project.deadline ?? '',
      status: project.status ?? '',
      category: project.category ?? '',
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingProject = null;
  }

  saveEdit(): void {
    if (!this.editingProject?.id || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const payload: Partial<Project> = {
      id: this.editingProject.id,
      clientId: v.clientId != null ? Number(v.clientId) : undefined,
      title: v.title.trim(),
      description: v.description.trim(),
      budget: v.budget != null && v.budget !== '' ? Number(v.budget) : undefined,
      deadline: v.deadline?.trim() || undefined,
      status: v.status?.trim() || undefined,
      category: v.category?.trim() || undefined,
    };
    this.saving = true;
    this.projectService.updateProject(payload).subscribe({
      next: (updated) => {
        this.saving = false;
        if (updated) {
          const idx = this.projects.findIndex((p) => p.id === this.editingProject!.id);
          if (idx !== -1) this.projects[idx] = { ...this.projects[idx], ...updated };
          this.editingProject = null;
        } else this.errorMessage = 'Failed to update project.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update project.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(project: Project): void {
    this.projectToDelete = project;
    this.closeEdit();
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.projectToDelete = null;
  }

  doDelete(): void {
    if (!this.projectToDelete?.id) return;
    this.deleting = true;
    this.projectService.deleteProject(this.projectToDelete.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.projectToDelete = null;
        if (ok) this.loadProjects();
        else this.errorMessage = 'Failed to delete project.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete project.';
        this.cdr.detectChanges();
      },
    });
  }
}
