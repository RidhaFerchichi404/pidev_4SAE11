import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  form: FormGroup;
  errorMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  /** Contrôle de saisie: get user-facing error for email field. */
  getEmailError(): string {
    const c = this.form.get('email');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Email is required.';
    if (c.errors['email']) return 'Please enter a valid email address.';
    return '';
  }

  /** Contrôle de saisie: get user-facing error for password field. */
  getPasswordError(): string {
    const c = this.form.get('password');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Password is required.';
    if (c.errors['minlength']) return 'Password must be at least 8 characters.';
    return '';
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.loading = true;
    this.auth.login(email, password).subscribe({
      next: (res) => {
        this.loading = false;
        if (res && 'access_token' in res && res.access_token) {
          const role = this.auth.getUserRole();
          if (role === 'ADMIN') {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        } else if (res && 'error' in res) {
          this.errorMessage = res.error;
        } else {
          this.errorMessage = 'Invalid email or password. Please try again.';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.error ?? err?.message ?? 'Login failed. Please try again.';
      },
    });
  }
}
