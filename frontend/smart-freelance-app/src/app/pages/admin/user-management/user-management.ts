import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService, User, UserRequest, UserRole } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { Card } from '../../../shared/components/card/card';

const ROLES: UserRole[] = ['CLIENT', 'FREELANCER', 'ADMIN'];

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './user-management.html',
  styleUrl: './user-management.scss',
})
export class UserManagement implements OnInit {
  users: User[] = [];
  loading = true;
  errorMessage = '';
  editForm: FormGroup;
  addUserForm: FormGroup;
  editingUser: User | null = null;
  saving = false;
  addingUser = false;
  addUserModalOpen = false;
  addUserAvatarUploading = false;
  userToDelete: User | null = null;
  deleting = false;
  avatarUploading = false;
  roles = ROLES;

  constructor(
    private userService: UserService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.editForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      phone: [''],
      avatarUrl: [''],
      role: ['CLIENT' as UserRole, Validators.required],
      isActive: [true],
      password: [''],
    });
    this.addUserForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      phone: [''],
      avatarUrl: [''],
      role: ['CLIENT' as UserRole, Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.userService.getAll().subscribe({
      next: (list) => {
        this.users = list ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        const status = err?.status;
        const msg = err?.error?.message ?? err?.error?.error;
        // On 401 the interceptor redirects to login; avoid updating view state to prevent NG0100
        if (status === 401) {
          this.loading = false;
          return;
        }
        // Defer other error updates to next tick to avoid ExpressionChangedAfterItHasBeenCheckedError
        setTimeout(() => {
          this.loading = false;
          if (typeof msg === 'string' && msg.length > 0) {
            this.errorMessage = msg;
          } else {
            if (status === 502) {
              this.errorMessage = 'Gateway could not reach the User service. Is the User microservice running on port 8090?';
            } else if (status === 500 || status === 503) {
              this.errorMessage = 'Server error. Ensure the User microservice is running and that MySQL (userdb) is available.';
            } else {
              this.errorMessage = 'Failed to load users. Check your connection and try again.';
            }
          }
        }, 0);
      },
    });
  }

  openEdit(user: User): void {
    this.editingUser = user;
    this.userToDelete = null;
    this.editForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      phone: user.phone ?? '',
      avatarUrl: user.avatarUrl ?? '',
      role: user.role,
      isActive: user.isActive,
      password: '',
    });
  }

  openAddUser(): void {
    this.addUserModalOpen = true;
    this.errorMessage = '';
    this.addUserForm.reset({
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      phone: '',
      avatarUrl: '',
      role: 'CLIENT',
    });
  }

  closeAddUser(): void {
    if (!this.addingUser) this.addUserModalOpen = false;
  }

  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0];
    if (!file || !file.type.startsWith('image/')) return;
    this.avatarUploading = true;
    this.userService.uploadAvatar(file).subscribe({
      next: (url) => {
        this.avatarUploading = false;
        if (url) this.editForm.patchValue({ avatarUrl: url });
        this.cdr.detectChanges();
        input.value = '';
      },
      error: () => {
        this.avatarUploading = false;
        this.cdr.detectChanges();
        input.value = '';
      },
    });
  }

  onAddUserAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0];
    if (!file || !file.type.startsWith('image/')) return;
    this.addUserAvatarUploading = true;
    this.userService.uploadAvatar(file).subscribe({
      next: (url) => {
        this.addUserAvatarUploading = false;
        if (url) this.addUserForm.patchValue({ avatarUrl: url });
        this.cdr.detectChanges();
        input.value = '';
      },
      error: () => {
        this.addUserAvatarUploading = false;
        this.cdr.detectChanges();
        input.value = '';
      },
    });
  }

  get addUserAvatarPreviewUrl(): string | null {
    const url = this.addUserForm.get('avatarUrl')?.value;
    return (url && typeof url === 'string' && url.trim()) ? url.trim() : null;
  }

  saveAddUser(): void {
    if (this.addUserForm.invalid) {
      this.addUserForm.markAllAsTouched();
      return;
    }
    const value = this.addUserForm.getRawValue();
    this.addingUser = true;
    this.errorMessage = '';
    this.auth.adminCreateUser({
      email: value.email.trim(),
      password: value.password,
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      role: value.role,
      phone: value.phone?.trim() || undefined,
      avatarUrl: value.avatarUrl?.trim() || undefined,
    }).subscribe({
      next: (res) => {
        this.addingUser = false;
        if (res && 'error' in res) {
          this.errorMessage = res.error;
          this.cdr.detectChanges();
          return;
        }
        this.addUserModalOpen = false;
        this.loadUsers();
        this.cdr.detectChanges();
      },
      error: () => {
        this.addingUser = false;
        this.errorMessage = 'Failed to create user.';
        this.cdr.detectChanges();
      },
    });
  }

  get editAvatarPreviewUrl(): string | null {
    const url = this.editForm.get('avatarUrl')?.value;
    return (url && typeof url === 'string' && url.trim()) ? url.trim() : null;
  }

  closeEdit(): void {
    this.editingUser = null;
    this.avatarUploading = false;
    this.editForm.reset({
      firstName: '',
      lastName: '',
      phone: '',
      avatarUrl: '',
      role: 'CLIENT',
      isActive: true,
      password: '',
    });
  }

  saveEdit(): void {
    if (!this.editingUser || this.editForm.invalid) return;
    const value = this.editForm.getRawValue();
    const body: Partial<UserRequest> = {
      firstName: value.firstName,
      lastName: value.lastName,
      phone: value.phone ?? undefined,
      avatarUrl: value.avatarUrl ?? undefined,
      role: value.role,
      isActive: value.isActive,
    };
    if (value.password && value.password.length >= 8) {
      body.password = value.password;
    }
    this.saving = true;
    this.userService.update(this.editingUser.id, body).subscribe({
      next: (updated) => {
        this.saving = false;
        if (updated) {
          const idx = this.users.findIndex((u) => u.id === this.editingUser!.id);
          if (idx !== -1) this.users[idx] = updated;
          this.closeEdit();
        } else {
          this.errorMessage = 'Failed to update user.';
        }
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update user.';
      },
    });
  }

  openDeleteModal(user: User): void {
    this.userToDelete = user;
    this.closeEdit();
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.userToDelete = null;
  }

  doDelete(): void {
    const user = this.userToDelete;
    if (!user) return;
    this.deleting = true;
    this.userService.delete(user.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.userToDelete = null;
        if (ok) {
          this.users = this.users.filter((u) => u.id !== user.id);
        } else {
          this.errorMessage = 'Failed to delete user.';
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.deleting = false;
        this.userToDelete = null;
        const msg = err?.error?.error ?? err?.error?.message;
        this.errorMessage = typeof msg === 'string' && msg.length > 0 ? msg : 'Failed to delete user.';
        this.cdr.detectChanges();
      },
    });
  }

  roleLabel(role: string): string {
    return role.charAt(0) + role.slice(1).toLowerCase();
  }
}
