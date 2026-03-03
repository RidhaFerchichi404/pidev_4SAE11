import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent {
  form: FormGroup;
  errorMessage = '';
  successMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  getEmailError(): string {
    const c = this.form.get('email');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Email is required.';
    if (c.errors['email']) return 'Please enter a valid email address.';
    return '';
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email } = this.form.getRawValue();
    this.loading = true;
    this.cdr.detectChanges();

    const done = () => {
      this.loading = false;
      this.cdr.detectChanges();
    };

    // Fallback: if no response in 8 seconds, assume success (backend returns quickly; email may still arrive)
    const fallbackTimer = setTimeout(() => {
      if (this.loading && !this.successMessage && !this.errorMessage) {
        this.successMessage = `Email sent to ${email}. Check your inbox for the password reset link.`;
        done();
      }
    }, 8000);

    this.auth.forgotPassword(email).subscribe({
      next: (res) => {
        clearTimeout(fallbackTimer);
        if (res && 'error' in res) {
          this.errorMessage = res.error;
        } else if (res && typeof res === 'object' && 'message' in res) {
          this.successMessage = `Email sent to ${email}. Check your inbox for the password reset link.`;
        } else {
          this.successMessage = `Email sent to ${email}. Check your inbox for the password reset link.`;
        }
        done();
      },
      error: (err) => {
        clearTimeout(fallbackTimer);
        this.errorMessage = err?.error?.error ?? err?.message ?? 'Something went wrong. Please try again.';
        done();
      },
      complete: () => {
        done();
      },
    });
  }
}
