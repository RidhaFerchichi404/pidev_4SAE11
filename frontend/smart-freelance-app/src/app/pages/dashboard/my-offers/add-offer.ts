import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, OfferRequest } from '../../../core/services/offer.service';

export const CATEGORIES = [
  'Frontend', 'Backend', 'Full Stack', 'UI/UX',
  'SEO', 'Content', 'Machine Learning', 'Cloud',
];

@Component({
  selector: 'app-add-offer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './add-offer.html',
  styleUrl: './add-offer.scss',
})
export class AddOffer implements OnInit {
  form: FormGroup;
  isSubmitting = false;
  readonly categories = CATEGORIES;
  submitError: string | null = null;
  currentUser: User | null = null;
  minDate: string;

  translateLang: 'fr' | 'en' | 'ar' = 'en';
  translating = false;
  translateFormError = '';
  translatedFormResult: string[] | null = null;

  /** Barre de notification (success / error / info) */
  notificationMessage = '';
  notificationType: 'success' | 'error' | 'info' = 'info';
  notificationVisible = false;
  private notificationTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private router: Router
  ) {
    const t = new Date();
    this.minDate = t.toISOString().slice(0, 10);
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
      domain: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(5000)]],
      price: [null, [Validators.required, Validators.min(0.01)]],
      durationType: ['fixed', [Validators.required]],
      deadline: [''],
      category: [''],
      tags: [''],
    });
  }

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.showNotification('You must be logged in.', 'error');
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      if (!this.currentUser) this.showNotification('Could not load your profile.', 'error');
    });
  }

  showNotification(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    if (this.notificationTimeout) clearTimeout(this.notificationTimeout);
    this.notificationMessage = message;
    this.notificationType = type;
    this.notificationVisible = true;
    if (type === 'success' || type === 'info') {
      this.notificationTimeout = setTimeout(() => this.clearNotification(), 5000);
    }
  }

  clearNotification(): void {
    if (this.notificationTimeout) {
      clearTimeout(this.notificationTimeout);
      this.notificationTimeout = null;
    }
    this.notificationVisible = false;
    this.notificationMessage = '';
  }

  canTranslate(): boolean {
    const t = this.form.get('title')?.value?.trim();
    const d = this.form.get('description')?.value?.trim();
    return !!(t && t.length >= 5 && d && d.length >= 20);
  }

  runTranslateForm(): void {
    if (!this.canTranslate()) return;
    const title = this.form.get('title')?.value ?? '';
    const description = this.form.get('description')?.value ?? '';
    this.translating = true;
    this.translateFormError = '';
    this.translatedFormResult = null;
    this.offerService.translateTexts([title, description], this.translateLang).subscribe({
      next: (translations) => {
        this.translatedFormResult = translations;
        this.translating = false;
      },
      error: (err: unknown) => {
        const e = err as { error?: { message?: string }; message?: string };
        this.translateFormError = e?.error?.message || e?.message || 'Translation failed.';
        this.translating = false;
      },
    });
  }

  applyTranslation(): void {
    if (!this.translatedFormResult || this.translatedFormResult.length < 2) return;
    this.form.patchValue({
      title: this.translatedFormResult[0],
      description: this.translatedFormResult[1],
    });
    this.translatedFormResult = null;
    this.showNotification('Translation applied to the form.', 'success');
  }

  submit(): void {
    if (this.form.invalid || !this.currentUser?.id) return;
    this.form.markAllAsTouched();
    this.isSubmitting = true;
    this.submitError = null;

    const v = this.form.value;
    const req: OfferRequest = {
      freelancerId: this.currentUser.id,
      title: (v.title as string).trim(),
      domain: (v.domain as string).trim(),
      description: (v.description as string).trim(),
      price: Number(v.price),
      durationType: v.durationType,
      deadline: v.deadline ? (v.deadline as string) : undefined,
      category: (v.category as string)?.trim() || undefined,
      tags: (v.tags as string)?.trim() || undefined,
    };

    this.offerService.createOffer(req).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.router.navigate(['/dashboard/my-offers']);
      },
      error: (err: unknown) => {
        this.isSubmitting = false;
        const e = err as { error?: { message?: string }; message?: string };
        this.showNotification(e?.error?.message || e?.message || 'Failed to create offer.', 'error');
      },
    });
  }
}
