import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../../core/services/auth.service';

const ROLES = ['CLIENT', 'FREELANCER', 'ADMIN'];

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignupComponent {
  form: FormGroup;
  errorMessage = '';
  successMessage = '';
  loading = false;
  roles = ROLES;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      role: ['CLIENT', Validators.required],
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.form.invalid) {
      console.warn('[Signup] Submit skipped: form invalid', this.form.errors, this.form.value);
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: RegisterRequest = {
      email: value.email,
      password: value.password,
      firstName: value.firstName,
      lastName: value.lastName,
      role: value.role,
    };
    console.log('[Signup] Submitting registration for', request.email, 'role:', request.role);
    this.loading = true;
    this.auth.register(request).subscribe({
      next: (res) => {
        this.loading = false;
        if (res && 'error' in res) {
          this.errorMessage = res.error;
          console.warn('[Signup] Registration failed (backend):', res.error);
        } else if (res?.message) {
          console.log('[Signup] Registration succeeded:', res.message);
          this.successMessage = 'Account created. You can now sign in.';
          this.form.reset({ role: 'CLIENT' });
        } else {
          this.errorMessage = 'Registration failed. Please try again.';
        }
      },
      error: (err) => {
        this.loading = false;
        console.error('[Signup] Registration error (subscribe):', err);
        this.errorMessage = err?.error?.error || err?.message || 'Registration failed. Please try again.';
      },
    });
  }
}
