import { Component, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, readAccessTokenFromBody } from '../../core/services/auth.service';
import { finalize } from 'rxjs';

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
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      staySignedIn: [true],
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
    const { email, password, staySignedIn } = this.form.getRawValue();
    this.loading = true;
    this.auth.login(email, password, !!staySignedIn).pipe(
      finalize(() => {
        this.ngZone.run(() => {
          this.loading = false;
          this.cdr.detectChanges();
        });
      })
    ).subscribe({
      next: (res) => {
        if (readAccessTokenFromBody(res)) {
          const role = this.auth.getUserRole();
          if (role === 'ADMIN') {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        } else {
          this.errorMessage = (res && 'error' in res) ? res.error : 'Invalid email or password. Please try again.';
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.loading = false;
        const body = err?.error;
        this.errorMessage =
          (typeof body === 'object' && body?.error ? body.error : typeof body === 'string' ? body : null)
          ?? err?.message
          ?? 'Login failed. Please try again.';
      },
    });
  }
}
