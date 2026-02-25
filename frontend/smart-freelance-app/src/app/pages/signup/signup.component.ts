import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';

const ROLES = ['CLIENT', 'FREELANCER', 'ADMIN'];

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignupComponent implements OnDestroy {
  form: FormGroup;
  errorMessage = '';
  successMessage = '';
  loading = false;
  checkingEmail = false;
  /** Shown while uploading avatar after signup (deferred upload). */
  avatarUploading = false;
  roles = ROLES;
  /** Avatar file selected on signup is uploaded after registration (no auth during signup). */
  pendingAvatarFile: File | null = null;
  private pendingAvatarObjectUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private userService: UserService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      role: ['CLIENT', Validators.required],
      phone: [''],
      avatarUrl: [''],
    });
  }

  /** Contrôle de saisie: message for email field. */
  getEmailError(): string {
    const c = this.form.get('email');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Email is required.';
    if (c.errors['email']) return 'Please enter a valid email address.';
    return '';
  }

  /** Contrôle de saisie: message for password field. */
  getPasswordError(): string {
    const c = this.form.get('password');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Password is required.';
    if (c.errors['minlength']) return 'Password must be at least 8 characters.';
    return '';
  }

  /** Contrôle de saisie: message for first name. */
  getFirstNameError(): string {
    const c = this.form.get('firstName');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'First name is required.';
    return '';
  }

  /** Contrôle de saisie: message for last name. */
  getLastNameError(): string {
    const c = this.form.get('lastName');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Last name is required.';
    return '';
  }

  /** Store avatar file for upload after signup (avatar endpoint requires auth). */
  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0];
    if (!file || !file.type.startsWith('image/')) return;
    this.clearPendingAvatar();
    this.pendingAvatarFile = file;
    this.pendingAvatarObjectUrl = URL.createObjectURL(file);
    this.errorMessage = '';
    input.value = '';
  }

  clearPendingAvatar(): void {
    if (this.pendingAvatarObjectUrl) {
      URL.revokeObjectURL(this.pendingAvatarObjectUrl);
      this.pendingAvatarObjectUrl = null;
    }
    this.pendingAvatarFile = null;
  }

  ngOnDestroy(): void {
    this.clearPendingAvatar();
  }

  get avatarPreviewUrl(): string | null {
    if (this.pendingAvatarObjectUrl) return this.pendingAvatarObjectUrl;
    const url = this.form.get('avatarUrl')?.value;
    return (url && typeof url === 'string' && url.trim()) ? url.trim() : null;
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();

    this.loading = true;
    this.checkingEmail = true;
    this.userService.getByEmail(value.email).subscribe({
      next: (existing) => {
        this.checkingEmail = false;
        if (existing) {
          this.loading = false;
          this.errorMessage = 'An account with this email already exists. Try signing in or use another email.';
          return;
        }
        this.doRegister(value);
      },
      error: () => {
        this.checkingEmail = false;
        this.loading = false;
        this.errorMessage = 'Unable to verify email. Please try again.';
      },
    });
  }

  private doRegister(value: { email: string; password: string; firstName: string; lastName: string; role: string; phone: string; avatarUrl: string }): void {
    const request: RegisterRequest = {
      email: value.email,
      password: value.password,
      firstName: value.firstName,
      lastName: value.lastName,
      role: value.role,
      phone: value.phone?.trim() || undefined,
      avatarUrl: value.avatarUrl?.trim() || undefined,
    };
    this.auth.register(request).subscribe({
      next: (res) => {
        if (res && 'error' in res) {
          this.loading = false;
          this.errorMessage = res.error;
          return;
        }
        if (!res?.message) {
          this.loading = false;
          this.errorMessage = 'Registration failed. Please try again.';
          return;
        }
        const file = this.pendingAvatarFile;
        if (file) {
          this.loading = false;
          this.avatarUploading = true;
          this.auth.login(value.email, value.password).subscribe({
            next: (loginRes) => {
              if (loginRes && 'error' in loginRes) {
                this.avatarUploading = false;
                this.clearPendingAvatar();
                this.successMessage = 'Account created. You can now sign in.';
                this.form.reset({ role: 'CLIENT', phone: '', avatarUrl: '' });
                return;
              }
              const userId = this.auth.getUserId();
              if (userId) {
                this.userService.uploadAvatar(file).subscribe({
                  next: (url) => {
                    if (url) {
                      this.userService.update(userId, { avatarUrl: url }).subscribe();
                    }
                    this.avatarUploading = false;
                    this.clearPendingAvatar();
                    this.successMessage = 'Account created. You can now sign in.';
                    this.form.reset({ role: 'CLIENT', phone: '', avatarUrl: '' });
                  },
                  error: () => {
                    this.avatarUploading = false;
                    this.clearPendingAvatar();
                    this.successMessage = 'Account created. You can now sign in.';
                    this.form.reset({ role: 'CLIENT', phone: '', avatarUrl: '' });
                  },
                });
              } else {
                this.avatarUploading = false;
                this.clearPendingAvatar();
                this.successMessage = 'Account created. You can now sign in.';
                this.form.reset({ role: 'CLIENT', phone: '', avatarUrl: '' });
              }
            },
            error: () => {
              this.avatarUploading = false;
              this.clearPendingAvatar();
              this.successMessage = 'Account created. You can now sign in.';
              this.form.reset({ role: 'CLIENT', phone: '', avatarUrl: '' });
            },
          });
        } else {
          this.loading = false;
          this.successMessage = 'Account created. You can now sign in.';
          this.form.reset({ role: 'CLIENT', phone: '', avatarUrl: '' });
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.error || err?.message || 'Registration failed. Please try again.';
      },
    });
  }
}
