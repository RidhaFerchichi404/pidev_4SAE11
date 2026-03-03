import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { OfferService, Offer, OfferApplication, OfferQuestionResponse } from '../../../core/services/offer.service';

@Component({
  selector: 'app-show-offer',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './show-offer.html',
  styleUrl: './show-offer.scss',
})
export class ShowOffer implements OnInit {
  offer: Offer | null = null;
  applications: OfferApplication[] = [];
  loading = true;
  loadingApps = false;
  errorMessage = '';
  actioning = false;
  currentUser: User | null = null;

  selectedTranslateLang: 'fr' | 'en' | 'ar' = 'en';
  translating = false;
  translateError = '';
  translatedResult: { title: string; description: string } | null = null;

  offerQuestions: OfferQuestionResponse[] = [];
  loadingQuestions = false;
  answerTexts: Record<number, string> = {};
  answeringId: number | null = null;

<<<<<<< HEAD
=======
  // ── Offer Quality Score ──────────────────────────────────────
  scoreExpanded = true;

  get qualityScore(): number {
    const o = this.offer;
    if (!o) return 0;
    let score = 0;

    // Title: ideal 20-80 chars (20 pts)
    const titleLen = (o.title || '').trim().length;
    if (titleLen >= 20 && titleLen <= 80) score += 20;
    else if (titleLen >= 8) score += 12;
    else if (titleLen >= 3) score += 5;

    // Description: 200+ chars ideal (25 pts)
    const descLen = (o.description || '').trim().length;
    if (descLen >= 300) score += 25;
    else if (descLen >= 150) score += 18;
    else if (descLen >= 80) score += 10;
    else if (descLen > 0) score += 5;

    // Price set (15 pts)
    if (o.price && o.price > 0) score += 15;

    // Domain/Category (15 pts)
    if ((o.domain || '').trim().length > 0) score += 15;

    // Tags (10 pts)
    const tags = (o.tags || '').split(',').map((t: string) => t.trim()).filter(Boolean);
    if (tags.length >= 3) score += 10;
    else if (tags.length >= 1) score += 5;

    // Image (10 pts)
    if ((o.imageUrl || '').trim().length > 0) score += 10;

    // Packages: basic + standard + premium (5 pts)
    if (o.basicPrice && o.standardPrice && o.premiumPrice) score += 5;

    return Math.min(score, 100);
  }

  get scoreLevel(): { label: string; color: string; emoji: string } {
    const s = this.qualityScore;
    if (s >= 85) return { label: 'Top Rated', color: '#10b981', emoji: '🏆' };
    if (s >= 65) return { label: 'Rising Talent', color: '#f59e0b', emoji: '⭐' };
    if (s >= 40) return { label: 'Getting Started', color: '#3b82f6', emoji: '📈' };
    return { label: 'Needs Work', color: '#ef4444', emoji: '🔧' };
  }

  get scoreDashoffset(): number {
    const circumference = 2 * Math.PI * 52;
    return circumference - (this.qualityScore / 100) * circumference;
  }

  get scoreChecks(): { label: string; tip: string; ok: boolean; pts: number }[] {
    const o = this.offer;
    if (!o) return [];
    const titleLen = (o.title || '').trim().length;
    const descLen = (o.description || '').trim().length;
    const tags = (o.tags || '').split(',').map((t: string) => t.trim()).filter(Boolean);
    return [
      {
        label: 'Titre optimisé',
        tip: 'Idéalement entre 20 et 80 caractères, descriptif et accrocheur.',
        ok: titleLen >= 20 && titleLen <= 80,
        pts: 20,
      },
      {
        label: 'Description complète',
        tip: 'Minimum 200 caractères recommandés. Décrivez votre service en détail.',
        ok: descLen >= 150,
        pts: 25,
      },
      {
        label: 'Prix défini',
        tip: 'Ajoutez un prix de base pour attirer des clients.',
        ok: !!(o.price && o.price > 0),
        pts: 15,
      },
      {
        label: 'Catégorie renseignée',
        tip: 'Choisissez un domaine précis pour être mieux référencé.',
        ok: (o.domain || '').trim().length > 0,
        pts: 15,
      },
      {
        label: 'Tags (≥ 3)',
        tip: 'Ajoutez au moins 3 tags pour améliorer la visibilité.',
        ok: tags.length >= 3,
        pts: 10,
      },
      {
        label: "Image de l'offre",
        tip: 'Une image professionnelle augmente les clics de 40%.',
        ok: (o.imageUrl || '').trim().length > 0,
        pts: 10,
      },
      {
        label: 'Packages (Basic/Standard/Premium)',
        tip: 'Proposez 3 niveaux de service pour maximiser vos revenus.',
        ok: !!(o.basicPrice && o.standardPrice && o.premiumPrice),
        pts: 5,
      },
    ];
  }

  /** Modal rejet avec raison */
  rejectModalOpen = false;
  rejectApp: OfferApplication | null = null;
  rejectReason = '';
  rejectError: string | null = null;

>>>>>>> fc652c4 (le nouveau version)
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((u) => {
      this.currentUser = u ?? null;
      if (!this.currentUser) {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
        return;
      }
      this.offerService.getOfferById(id).subscribe((offer) => {
        this.offer = offer ?? null;
        this.loading = false;
        if (this.offer && this.currentUser != null && this.offer.freelancerId === this.currentUser.id) {
          this.loadApplications();
          this.loadQuestions();
        } else if (this.offer) {
          this.errorMessage = 'You do not own this offer.';
        } else {
          this.errorMessage = 'Offer not found.';
        }
        this.cdr.detectChanges();
      });
    });
  }

  loadApplications(): void {
    if (!this.offer?.id || !this.currentUser?.id) return;
    this.loadingApps = true;
    this.offerService.getApplicationsByOffer(this.offer.id, 0, 50).subscribe((page) => {
      this.applications = page.content ?? [];
      this.loadingApps = false;
      this.cdr.detectChanges();
    });
  }

  formatDate(s: string): string {
    if (!s) return '';
    const d = new Date(s);
    return d.toLocaleDateString();
  }

  accept(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.actioning = true;
    this.offerService.acceptApplication(app.id, this.currentUser.id).subscribe(() => {
      this.actioning = false;
      this.loadApplications();
      this.cdr.detectChanges();
    });
  }

  reject(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.actioning = true;
    this.offerService.rejectApplication(app.id, this.currentUser.id).subscribe(() => {
      this.actioning = false;
      this.loadApplications();
      this.cdr.detectChanges();
    });
  }

  createProject(app: OfferApplication): void {
    if (!this.offer) return;
    this.router.navigate(['/dashboard/my-projects/add'], {
      state: { fromOffer: true, offer: this.offer, application: app },
    });
  }

  runTranslate(): void {
    if (!this.offer?.id) return;
    this.translating = true;
    this.translateError = '';
    this.translatedResult = null;
    this.offerService.translateOffer(this.offer.id, this.selectedTranslateLang).subscribe({
      next: (res) => {
        this.translatedResult = { title: res.title, description: res.description };
        this.translating = false;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        const e = err as { error?: { message?: string }; message?: string };
        this.translateError = e?.error?.message || e?.message || 'Translation failed. Check API key configuration.';
        this.translating = false;
        this.cdr.detectChanges();
      },
    });
  }

  translateLangLabel(lang: string): string {
    const labels: Record<string, string> = { fr: 'French', en: 'English', ar: 'Arabic' };
    return labels[lang] || lang;
  }

  loadQuestions(): void {
    if (!this.offer?.id) return;
    this.loadingQuestions = true;
    this.offerService.getOfferQuestions(this.offer.id).subscribe((list) => {
      this.offerQuestions = list ?? [];
      this.loadingQuestions = false;
      this.cdr.detectChanges();
    });
  }

  submitAnswer(questionId: number): void {
    if (!this.currentUser?.id) return;
    const text = (this.answerTexts[questionId] || '').trim();
    if (!text) return;
    this.answeringId = questionId;
    this.offerService.answerOfferQuestion(questionId, this.currentUser.id, text).subscribe({
      next: (updated) => {
        this.answeringId = null;
        if (updated) {
          const idx = this.offerQuestions.findIndex((q) => q.id === questionId);
          if (idx >= 0) this.offerQuestions[idx] = updated;
          delete this.answerTexts[questionId];
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.answeringId = null;
        this.cdr.detectChanges();
      },
    });
  }
}
