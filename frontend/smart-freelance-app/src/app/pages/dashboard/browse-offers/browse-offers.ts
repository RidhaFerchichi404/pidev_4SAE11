import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import {
  OfferService,
  Offer,
  OfferFilterRequest,
  OfferStatus,
  PageResponse,
} from '../../../core/services/offer.service';
import { ChatAssistantService, ChatMessageDto } from '../../../core/services/chat-assistant.service';
import { ChatMarkdownPipe } from '../../../shared/pipes/chat-markdown.pipe';

export const CATEGORIES = [
  'Frontend', 'Backend', 'Full Stack', 'UI/UX',
  'Mobile', 'SEO', 'Content', 'Machine Learning',
  'Cloud', 'DevOps', 'Data Science', 'Design',
];

export const SORT_OPTIONS = [
  { label: 'Newest first',       sortBy: 'createdAt', dir: 'DESC' },
  { label: 'Oldest first',       sortBy: 'createdAt', dir: 'ASC'  },
  { label: 'Price: Low → High',  sortBy: 'price',     dir: 'ASC'  },
  { label: 'Price: High → Low',  sortBy: 'price',     dir: 'DESC' },
  { label: 'Top Rated',          sortBy: 'rating',    dir: 'DESC' },
];

const PAGE_SIZE = 12;

@Component({
  selector: 'app-browse-offers',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule, ChatMarkdownPipe],
  templateUrl: './browse-offers.html',
  styleUrl: './browse-offers.scss',
})
export class BrowseOffers implements OnInit, OnDestroy {

  offers: Offer[] = [];
  recommendedOffers: Offer[] = [];
  loading = false;
  loadingRecommendations = false;
  errorMessage = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  currentUser: User | null = null;
  showAdvanced = false;

  searchForm!: FormGroup;
  readonly categories  = CATEGORIES;
  readonly sortOptions = SORT_OPTIONS;

  private destroy$ = new Subject<void>();
  private formSub: Subscription | null = null;

  constructor(
    private offerService: OfferService,
    private chatAssistant: ChatAssistantService,
    private auth: AuthService,
    private userService: UserService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.searchForm = this.fb.group({
      keyword:   [''],
      category:  [''],
      minPrice:  [null],
      maxPrice:  [null],
      sortIndex: [0],
    });

    // Subscribe to form changes with debounce for live search
    this.formSub = this.searchForm.valueChanges.pipe(
      debounceTime(350),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadOffers();
    });

    const email = this.auth.getPreferredUsername();
    if (email) {
      this.userService.getByEmail(email).subscribe((u) => {
        this.currentUser = u ?? null;
        if (this.currentUser?.id) this.loadRecommendations();
        this.cdr.detectChanges();
      });
    }

    this.loadOffers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.formSub?.unsubscribe();
  }

  // ── Computed helpers ──────────────────────────────────────────

  get hasActiveFilters(): boolean {
    const v = this.searchForm?.value;
    if (!v) return false;
    return !!(v.keyword || v.category || v.minPrice || v.maxPrice || Number(v.sortIndex) !== 0);
  }

  toggleAdvanced(): void {
    this.showAdvanced = !this.showAdvanced;
  }

  resetFilters(): void {
    this.searchForm.reset({ keyword: '', category: '', minPrice: null, maxPrice: null, sortIndex: 0 });
    this.currentPage = 0;
    this.loadOffers();
  }

  // ── Build filter from form ────────────────────────────────────

  private buildFilter(): OfferFilterRequest {
    const v = this.searchForm.value;
    const sortOpt = SORT_OPTIONS[Number(v.sortIndex) ?? 0] ?? SORT_OPTIONS[0];

    const f: OfferFilterRequest = {
      offerStatus: 'AVAILABLE' as OfferStatus,
      page: this.currentPage,
      size: PAGE_SIZE,
      sortBy: sortOpt.sortBy,
      sortDirection: sortOpt.dir as 'ASC' | 'DESC',
    };

    if (v.keyword?.trim())  f.keyword   = v.keyword.trim();
    if (v.category)         f.category  = v.category;
    if (v.minPrice != null) f.minPrice  = v.minPrice;
    if (v.maxPrice != null) f.maxPrice  = v.maxPrice;

    return f;
  }

  // ── Load offers ───────────────────────────────────────────────

  loadOffers(): void {
    this.loading = true;
    this.errorMessage = '';

    const obs$ = this.hasActiveFilters
      ? this.offerService.searchOffers(this.buildFilter())
      : this.offerService.getActiveOffers(this.currentPage, PAGE_SIZE);

    obs$.subscribe({
      next: (res: PageResponse<Offer>) => {
        console.log('[BrowseOffers] loaded:', res?.totalElements, 'offers', res);
        this.offers = res?.content ?? [];
        this.totalElements = res?.totalElements ?? 0;
        this.totalPages = res?.totalPages ?? 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[BrowseOffers] error loading offers:', err);
        const status = err?.status;
        if (status === 0) {
          this.errorMessage = 'Le service des offres est inaccessible. Vérifie que le microservice Offer est bien démarré.';
        } else if (status === 500) {
          this.errorMessage = `Erreur serveur (500) lors du chargement des offres. Consulte les logs du microservice Offer.`;
        } else if (status === 404) {
          this.errorMessage = `Endpoint introuvable (404). Vérifie la configuration de l'API Gateway.`;
        } else {
          this.errorMessage = `Erreur ${status ?? 'inconnue'} lors du chargement des offres.`;
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadRecommendations(): void {
    if (!this.currentUser?.id) return;
    this.loadingRecommendations = true;
    this.offerService.getRecommendedOffers(this.currentUser.id, 6).subscribe((list) => {
      this.recommendedOffers = list ?? [];
      this.loadingRecommendations = false;
      this.cdr.detectChanges();
    });
  }

  // ── Pagination ────────────────────────────────────────────────

  get pages(): number[] {
    const total = this.totalPages;
    const current = this.currentPage;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const set = new Set<number>();
    set.add(0);
    for (let i = Math.max(0, current - 2); i <= Math.min(total - 1, current + 2); i++) set.add(i);
    set.add(total - 1);
    return [...set];
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadOffers();
  }

  // ── Chatbot Assistant (aide aux clients) ─────────────────────────────

  chatOpen = false;
  chatMessages: { role: 'user' | 'bot'; text: string; fromApi?: boolean }[] = [];
  chatInput = '';
  chatSending = false;

  private readonly BOT_WELCOME = `Bonjour ! Je suis votre assistant pour parcourir les offres. Je peux vous aider à :
• Comprendre les projets et les offres
• Choisir une offre adaptée à votre besoin
• Préparer un Design Brief
• Obtenir des idées pour votre projet

Posez-moi une question ou cliquez sur une suggestion ci-dessous.`;

  toggleChat(): void {
    this.chatOpen = !this.chatOpen;
    if (this.chatOpen && this.chatMessages.length === 0) {
      this.chatMessages = [{ role: 'bot', text: this.BOT_WELCOME }];
      this.cdr.detectChanges();
    }
  }

  sendChatMessage(text?: string): void {
    const msg = (text || this.chatInput || '').trim();
    if (!msg || this.chatSending) return;

    this.chatMessages.push({ role: 'user', text: msg });
    this.chatInput = '';
    this.chatSending = true;
    this.cdr.detectChanges();

    // Historique sans le message actuel (évite doublon côté API)
    const previousMessages = this.chatMessages.slice(0, -1);
    const history: ChatMessageDto[] = previousMessages
      .slice(-10)
      .filter((m) => m.role && m.text)
      .map((m) => ({ role: m.role === 'user' ? 'user' : 'assistant', text: m.text }));

    this.chatAssistant.getReply({ message: msg, history }).subscribe((reply) => {
      const fromApi = reply != null && reply.length > 0;
      const text = fromApi ? reply : this.getBotReply(msg);
      this.chatMessages.push({ role: 'bot', text, fromApi: fromApi || undefined });
      this.chatSending = false;
      this.cdr.detectChanges();
    });
  }

  private getBotReply(userMessage: string): string {
    const m = userMessage.toLowerCase();
    if (/\b(choisir|choisir une offre|quelle offre|quel projet)\b/.test(m)) {
      return `Pour bien choisir une offre :
1. **Lisez la description** en détail pour vérifier qu’elle correspond à votre besoin.
2. **Vérifiez le domaine** (Frontend, Backend, Design…) et le type de mission (fixed, hourly).
3. **Regardez le prix et la durée** pour rester dans votre budget.
4. Utilisez le **Design Brief** pour décrire votre projet : les freelancers pourront mieux vous proposer des offres adaptées.
5. N’hésitez pas à **poser des questions** au freelancer avant de postuler.`;
    }
    if (/\b(brief|design brief|quoi brief)\b/.test(m)) {
      return `Le **Design Brief** est un formulaire qui vous permet de définir votre projet (identité visuelle, couleurs, type d’application, délais, budget…). Une fois créé, vous pouvez le joindre à votre candidature pour que le freelancer comprenne mieux votre besoin et vous propose une offre sur mesure. Créez-le depuis la bannière "Créer mon brief" sur cette page.`;
    }
    if (/\b(idée|idées|idée projet|suggestions)\b/.test(m)) {
      return `Quelques idées pour avancer sur votre projet :
• **Clarifiez l’objectif** : site vitrine, e‑commerce, app mobile, refonte…
• **Définissez le périmètre** : nombre de pages, fonctionnalités prioritaires.
• **Précisez le public** : qui utilisera le produit ?
• **Budget et délai** : cela aide à filtrer les offres (prix fixe ou à l’heure).
• Utilisez les **filtres** (catégorie, prix) pour affiner la liste des offres.`;
    }
    if (/\b(prix|tarif|budget|coût)\b/.test(m)) {
      return `Les offres affichent un **prix** (souvent en TND) et un **type** (Fixed = forfait, Hourly = à l’heure). Utilisez les filtres "Min Price" et "Max Price" dans la barre de recherche pour ne voir que les offres dans votre budget. Le prix peut varier selon l’expérience du freelancer et la complexité du projet.`;
    }
    if (/\b(postuler|candidature|appliquer)\b/.test(m)) {
      return `Pour postuler : cliquez sur **"View & Apply"** sur une offre, puis remplissez le formulaire de candidature. Vous pouvez joindre un **Design Brief** si vous en avez créé un. Décrivez votre besoin clairement pour avoir plus de réponses.`;
    }
    if (/\b(merci|ok|d'accord|super)\b/.test(m)) {
      return `Avec plaisir ! N’hésitez pas si vous avez d’autres questions. Bonne recherche d’offres. 🙂`;
    }
    if (/\b(web|internet|site web|site internet)\b/.test(m) || /(c'?est quoi|qu'?est-ce que)\s*(le\s+)?(web|internet)/.test(m)) {
      return `Le **web** (ou World Wide Web) désigne l’ensemble des sites et pages accessibles sur **Internet**. Pour votre projet, vous pouvez chercher ici des offres de création de **sites web**, **applications** ou **refonte** : utilisez les filtres par catégorie (Frontend, Full Stack, etc.) et les mots-clés dans la barre de recherche.`;
    }
    if (/\b(freelance|freelancer)\b/.test(m)) {
      return `Un **freelancer** est un professionnel qui travaille en indépendant. Sur cette plateforme, les freelancers proposent des **offres** (services, prix, délais). En parcourant les offres (Browse Offers), vous choisissez celle qui correspond à votre besoin, puis vous postulez. Vous pouvez aussi créer un **Design Brief** pour décrire votre projet.`;
    }
    if (/\b(offre|offres)\b/.test(m)) {
      return `Une **offre** est un service proposé par un freelancer (ex. : création de site web, design, développement). Chaque offre a un titre, une description, un prix (TND) et un type (forfait ou à l’heure). Utilisez la recherche et les filtres pour trouver l’offre adaptée, puis cliquez sur **View & Apply** pour postuler.`;
    }
    return `Je n’ai pas de réponse précise pour « ${userMessage} ». Vous pouvez me demander par exemple : comment choisir une offre, c’est quoi un Design Brief, c’est quoi le web, ou des idées pour mon projet. Sinon, utilisez les filtres et la recherche pour explorer les offres.`;
  }
}
