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
  expandedQuestions = new Set<number>();
  qaSectionExpanded = true;

  toggleQaSection(): void {
    this.qaSectionExpanded = !this.qaSectionExpanded;
  }

  toggleQuestion(id: number): void {
    if (this.expandedQuestions.has(id)) {
      this.expandedQuestions.delete(id);
    } else {
      this.expandedQuestions.add(id);
    }
  }

  // ── Offer Quality Score ──────────────────────────────────────
  scoreExpanded = true;

  get qualityScore(): number {
    const o = this.offer;
    if (!o) return 0;
    let score = 0;

    const titleLen = (o.title || '').trim().length;
    if (titleLen >= 20 && titleLen <= 80) score += 20;
    else if (titleLen >= 8) score += 12;
    else if (titleLen >= 3) score += 5;

    const descLen = (o.description || '').trim().length;
    if (descLen >= 300) score += 25;
    else if (descLen >= 150) score += 18;
    else if (descLen >= 80) score += 10;
    else if (descLen > 0) score += 5;

    if (o.price && o.price > 0) score += 15;
    if ((o.domain || '').trim().length > 0) score += 15;

    const tags = (o.tags || '').split(',').map((t: string) => t.trim()).filter(Boolean);
    if (tags.length >= 3) score += 10;
    else if (tags.length >= 1) score += 5;

    if ((o.imageUrl || '').trim().length > 0) score += 10;

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

  get scoreDasharray(): string {
    return String(2 * Math.PI * 52);
  }

  get scoreChecks(): { label: string; tip: string; ok: boolean; pts: number }[] {
    const o = this.offer;
    if (!o) return [];
    const titleLen = (o.title || '').trim().length;
    const descLen = (o.description || '').trim().length;
    const tags = (o.tags || '').split(',').map((t: string) => t.trim()).filter(Boolean);
    return [
      { label: 'Titre optimisé',        tip: 'Idéalement entre 20 et 80 caractères.',           ok: titleLen >= 20 && titleLen <= 80, pts: 20 },
      { label: 'Description complète',  tip: 'Minimum 150 caractères recommandés.',             ok: descLen >= 150,                   pts: 25 },
      { label: 'Prix défini',           tip: 'Ajoutez un prix de base.',                        ok: !!(o.price && o.price > 0),       pts: 15 },
      { label: 'Catégorie renseignée',  tip: 'Choisissez un domaine précis.',                   ok: (o.domain || '').trim().length > 0, pts: 15 },
      { label: 'Tags (≥ 3)',            tip: 'Ajoutez au moins 3 tags.',                        ok: tags.length >= 3,                 pts: 10 },
      { label: "Image de l'offre",      tip: 'Une image augmente les clics de 40%.',            ok: (o.imageUrl || '').trim().length > 0, pts: 10 },
      { label: 'Prix compétitif',        tip: 'Définissez un prix attractif pour votre offre.',  ok: !!(o.price && o.price > 0),                            pts: 5 },
    ];
  }

  /** Modal rejet avec raison */
  rejectModalOpen = false;
  rejectApp: OfferApplication | null = null;
  rejectReason = '';
  rejectError: string | null = null;

  /** Error message for accept/reject actions */
  actionError: string | null = null;

  /** Modal publier offre */
  offerToPublish: Offer | null = null;
  publishing = false;

  // ── Design Brief parsing ─────────────────────────────────────
  private readonly BRIEF_MARKER = '── DESIGN BRIEF JOINT ──────────────────';

  /** True if the application message contains a design brief */
  hasBrief(message: string): boolean {
    return (message || '').includes(this.BRIEF_MARKER);
  }

  /** The personal text before the brief block */
  personalMessage(message: string): string {
    if (!this.hasBrief(message)) return message || '';
    return (message || '').split(this.BRIEF_MARKER)[0].trim();
  }

  /** Parsed key-value rows from the brief block */
  parseBriefRows(message: string): { label: string; value: string }[] {
    if (!this.hasBrief(message)) return [];
    const block = message.split(this.BRIEF_MARKER)[1] ?? '';
    const rows: { label: string; value: string }[] = [];
    const skip = ['────────────────────────────────────────', ''];
    for (const line of block.split('\n')) {
      const trimmed = line.trim();
      if (skip.some(s => trimmed.startsWith(s)) || !trimmed) continue;
      const colonIdx = trimmed.indexOf(' : ');
      if (colonIdx > -1) {
        rows.push({
          label: trimmed.substring(0, colonIdx).trim(),
          value: trimmed.substring(colonIdx + 3).trim(),
        });
      }
    }
    return rows;
  }

  /** Extract a specific field from parsed rows */
  briefField(message: string, label: string): string {
    return this.parseBriefRows(message).find(r => r.label === label)?.value ?? '';
  }

  /** Extract the two hex colors from "Couleurs" row */
  briefColors(message: string): { primary: string; secondary: string } {
    const raw = this.briefField(message, 'Couleurs');
    const parts = raw.split(' / ');
    return {
      primary:   parts[0]?.trim() || '#6C63FF',
      secondary: parts[1]?.trim() || '#FF6584',
    };
  }

  /** Initials for the logo mini-preview */
  briefInitials(message: string): string {
    const name = this.briefField(message, 'Projet');
    if (!name) return '?';
    const words = name.trim().split(/\s+/);
    return words.length >= 2
      ? (words[0][0] + words[1][0]).toUpperCase()
      : name.substring(0, 2).toUpperCase();
  }

  /** Pages as an array */
  briefPages(message: string): string[] {
    const raw = this.briefField(message, 'Pages');
    if (!raw) return [];
    return raw.split(', ').map(p => p.trim()).filter(Boolean);
  }

  // Track which app cards are expanded
  expandedApps = new Set<number>();

  toggleApp(id: number): void {
    if (this.expandedApps.has(id)) {
      this.expandedApps.delete(id);
    } else {
      this.expandedApps.add(id);
    }
  }

  countByStatus(status: string): number {
    return this.applications.filter(a => a.status === status).length;
  }

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
      // Auto-expand cards that contain a design brief
      this.applications.forEach(app => {
        if (this.hasBrief(app.message)) {
          this.expandedApps.add(app.id);
        }
      });
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
    this.actionError = null;
    this.offerService.acceptApplication(app.id, this.currentUser.id).subscribe({
      next: () => {
        this.actioning = false;
        this.loadApplications();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.actioning = false;
        const msg = err?.error?.message || err?.message || '';
        if (err?.status === 403 || msg.toLowerCase().includes('not authorized')) {
          this.actionError = 'Vous n\'êtes pas autorisé à accepter cette candidature.';
        } else if (err?.status === 400) {
          this.actionError = msg || 'Cette candidature ne peut pas être acceptée (statut invalide).';
        } else if (err?.status === 0) {
          this.actionError = 'Service inaccessible. Vérifiez que le microservice Offer est démarré.';
        } else {
          this.actionError = `Erreur ${err?.status ?? ''}: ${msg || 'Impossible d\'accepter la candidature.'}`;
        }
        this.cdr.detectChanges();
      },
    });
  }

  reject(app: OfferApplication): void {
    if (!this.currentUser?.id) return;
    this.actioning = true;
    this.actionError = null;
    this.offerService.rejectApplication(app.id, this.currentUser.id).subscribe({
      next: () => {
        this.actioning = false;
        this.loadApplications();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.actioning = false;
        const msg = err?.error?.message || err?.message || '';
        this.actionError = `Erreur lors du refus: ${msg || 'Impossible de refuser la candidature.'}`;
        this.cdr.detectChanges();
      },
    });
  }

  createProject(app: OfferApplication): void {
    if (!this.offer) return;
    this.router.navigate(['/dashboard/my-projects/add'], {
      state: { fromOffer: true, offer: this.offer, application: app },
    });
  }

  openPublishModal(offer: Offer): void {
    this.offerToPublish = offer;
  }

  closePublishModal(): void {
    if (!this.publishing) this.offerToPublish = null;
  }

  doPublish(): void {
    if (!this.offerToPublish || !this.currentUser?.id) return;
    this.publishing = true;
    this.offerService.publishOffer(this.offerToPublish.id, this.currentUser.id).subscribe({
      next: (updated) => {
        this.publishing = false;
        this.offerToPublish = null;
        if (updated) {
          this.offer = updated;
          this.loadApplications();
        } else {
          this.errorMessage = 'Échec de la publication.';
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.publishing = false;
        this.errorMessage = 'Échec de la publication.';
        this.cdr.detectChanges();
      },
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
