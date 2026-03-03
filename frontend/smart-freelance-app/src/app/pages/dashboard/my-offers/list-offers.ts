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
  AcceptanceRate,
  MonthlyEvolution,
} from '../../../core/services/offer.service';

<<<<<<< HEAD
const DEBOUNCE_MS = 350;
=======
export const CATEGORIES = [
  'Frontend', 'Backend', 'Full Stack', 'UI/UX',
  'SEO', 'Content', 'Machine Learning', 'Cloud',
];

export const SORT_OPTIONS = [
  { label: 'Newest first',      sortBy: 'createdAt', dir: 'DESC' },
  { label: 'Oldest first',      sortBy: 'createdAt', dir: 'ASC'  },
  { label: 'Price: Low → High', sortBy: 'price',     dir: 'ASC'  },
  { label: 'Price: High → Low', sortBy: 'price',     dir: 'DESC' },
];

export const OFFER_STATUSES = [
  { value: 'DRAFT',       label: 'Draft' },
  { value: 'AVAILABLE',   label: 'Available' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'ACCEPTED',    label: 'Accepted' },
  { value: 'COMPLETED',   label: 'Completed' },
  { value: 'EXPIRED',     label: 'Expired' },
  { value: 'CLOSED',      label: 'Closed' },
];

>>>>>>> fc652c4 (le nouveau version)
const PAGE_SIZE = 10;

@Component({
  selector: 'app-list-offers',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule],
  templateUrl: './list-offers.html',
  styleUrl: './list-offers.scss',
})
export class ListOffers implements OnInit, OnDestroy {
  offers: Offer[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  loading = true;
  errorMessage = '';
  currentUser: User | null = null;
  offerToDelete: Offer | null = null;
  deleting = false;
  offerToPublish: Offer | null = null;
  publishing = false;
  showAdvanced = false;

  searchForm!: FormGroup;
  readonly categories    = CATEGORIES;
  readonly sortOptions   = SORT_OPTIONS;
  readonly offerStatuses = OFFER_STATUSES;

  private destroy$ = new Subject<void>();
<<<<<<< HEAD
=======
  private formSub: Subscription | null = null;
>>>>>>> fc652c4 (le nouveau version)

  // Statistics
  statsByStatus: Record<string, number> | null = null;
  acceptanceRate: AcceptanceRate | null = null;
  monthlyEvolution: MonthlyEvolution | null = null;
  statsLoading = false;
  selectedYear = new Date().getFullYear();

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private offerService: OfferService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.searchForm = this.fb.group({
      keyword:     [''],
      category:    [''],
      offerStatus: [''],
      minPrice:    [null],
      maxPrice:    [null],
      sortIndex:   [0],
    });

    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }

    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (this.currentUser) {
        this.formSub = this.searchForm.valueChanges
          .pipe(
            debounceTime(350),
            distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
            takeUntil(this.destroy$),
          )
          .subscribe(() => {
            this.currentPage = 0;
            this.loadOffers();
          });
        this.loadOffers();
        this.loadStats();
      } else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.formSub?.unsubscribe();
  }

<<<<<<< HEAD
  private setupSearchDebounce(): void {
    this.keyword$
      .pipe(debounceTime(DEBOUNCE_MS), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadOffers();
      });
  }

  onKeywordInput(): void {
    this.keyword$.next(this.keyword);
  }

=======
  get hasActiveFilters(): boolean {
    const v = this.searchForm?.value;
    if (!v) return false;
    return !!(v.keyword || v.category || v.offerStatus || v.minPrice || v.maxPrice || Number(v.sortIndex) !== 0);
  }

  toggleAdvanced(): void {
    this.showAdvanced = !this.showAdvanced;
  }

  resetFilters(): void {
    this.searchForm.reset({ keyword: '', category: '', offerStatus: '', minPrice: null, maxPrice: null, sortIndex: 0 });
    this.currentPage = 0;
    this.loadOffers();
  }

>>>>>>> fc652c4 (le nouveau version)
  buildFilter(): OfferFilterRequest {
    const v = this.searchForm.value;
    const sortOpt = SORT_OPTIONS[Number(v.sortIndex) ?? 0] ?? SORT_OPTIONS[0];
    const f: OfferFilterRequest = {
      freelancerId: this.currentUser?.id,
      page: this.currentPage,
      size: PAGE_SIZE,
      sortBy: sortOpt.sortBy,
      sortDirection: sortOpt.dir as 'ASC' | 'DESC',
    };
    if (v.keyword?.trim())  f.keyword     = v.keyword.trim();
    if (v.category)         f.category    = v.category;
    if (v.offerStatus)      f.offerStatus = v.offerStatus as OfferStatus;
    if (v.minPrice != null) f.minPrice    = v.minPrice;
    if (v.maxPrice != null) f.maxPrice    = v.maxPrice;
    return f;
  }

  loadOffers(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.offerService.searchOffers(this.buildFilter()).subscribe({
      next: (res: PageResponse<Offer>) => {
        this.offers = res?.content ?? [];
        this.totalElements = res?.totalElements ?? 0;
        this.totalPages = res?.totalPages ?? 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load offers.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

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

  prev(): void {
    if (this.currentPage > 0) { this.currentPage--; this.loadOffers(); }
  }

  next(): void {
    if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.loadOffers(); }
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadOffers();
  }

  // ── Stats ───────────────────────────────────────────────────────────

  loadStats(): void {
    if (!this.currentUser?.id) return;
    this.statsLoading = true;
    this.offerService.getOffersCountByStatus(this.currentUser.id).subscribe({
      next: (res) => {
        this.statsByStatus = res?.countByStatus ?? null;
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
    });
    this.offerService.getAcceptanceRate(this.currentUser.id).subscribe({
      next: (res) => { this.acceptanceRate = res ?? null; this.cdr.detectChanges(); },
    });
    this.offerService.getMonthlyEvolution(this.currentUser.id, this.selectedYear).subscribe({
      next: (res) => { this.monthlyEvolution = res ?? null; this.cdr.detectChanges(); },
    });
  }

  refreshMonthlyEvolution(): void {
    if (!this.currentUser?.id) return;
    this.offerService.getMonthlyEvolution(this.currentUser.id, this.selectedYear).subscribe({
<<<<<<< HEAD
      next: (res) => {
        this.monthlyEvolution = res ?? null;
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(offer: Offer): void {
    this.offerToDelete = offer;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.offerToDelete = null;
  }

  doDelete(): void {
    if (!this.offerToDelete || !this.currentUser?.id) return;
    this.deleting = true;
    this.offerService.deleteOffer(this.offerToDelete.id, this.currentUser.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.offerToDelete = null;
        if (ok) {
          this.loadOffers();
          this.loadStats();
        } else this.errorMessage = 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.deleting = false;
        this.offerToDelete = null;
        this.errorMessage = err?.error?.message || 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
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
    this.offerService.publishOffer(this.offerToPublish.id, this.currentUser.id).subscribe((updated) => {
      this.publishing = false;
      this.offerToPublish = null;
      if (updated) {
        this.loadOffers();
        this.loadStats();
      } else this.errorMessage = 'Failed to publish offer.';
      this.cdr.detectChanges();
=======
      next: (res) => { this.monthlyEvolution = res ?? null; this.cdr.detectChanges(); },
>>>>>>> fc652c4 (le nouveau version)
    });
  }

  totalOffersByStatus(): number {
    if (!this.statsByStatus) return 0;
    return Object.values(this.statsByStatus).reduce((a, b) => a + b, 0);
  }

  statusSlug(key: string): string {
    return (key || '').toLowerCase().replace(/_/g, '-');
  }

  formatRate(rate: number): string {
    return (rate * 100).toFixed(1) + '%';
  }

  monthName(month: number): string {
    return ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][month - 1] ?? '';
  }

  get maxMonthCount(): number {
    if (!this.monthlyEvolution?.months?.length) return 1;
    return Math.max(1, ...this.monthlyEvolution.months.map((m) => m.count));
  }

  get yearOptions(): number[] {
    const y = new Date().getFullYear();
    return [y - 2, y - 1, y, y + 1];
  }

  // ── Modals ──────────────────────────────────────────────────────────

  canDeleteOffer(offer: Offer): boolean {
    return offer.offerStatus !== 'ACCEPTED' && offer.offerStatus !== 'IN_PROGRESS';
  }

  openDeleteModal(offer: Offer): void  { this.offerToDelete = offer; }
  closeDeleteModal(): void             { if (!this.deleting) this.offerToDelete = null; }

  doDelete(): void {
    if (!this.offerToDelete || !this.currentUser?.id) return;
    this.deleting = true;
    this.offerService.deleteOffer(this.offerToDelete.id, this.currentUser.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.offerToDelete = null;
        if (ok) { this.loadOffers(); this.loadStats(); }
        else this.errorMessage = 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.deleting = false;
        this.offerToDelete = null;
        this.errorMessage = err?.error?.message || 'Failed to delete offer.';
        this.cdr.detectChanges();
      },
    });
  }

  openPublishModal(offer: Offer): void  { this.offerToPublish = offer; }
  closePublishModal(): void             { if (!this.publishing) this.offerToPublish = null; }

  doPublish(): void {
    if (!this.offerToPublish || !this.currentUser?.id) return;
    this.publishing = true;
    this.offerService.publishOffer(this.offerToPublish.id, this.currentUser.id).subscribe((updated) => {
      this.publishing = false;
      this.offerToPublish = null;
      if (updated) { this.loadOffers(); this.loadStats(); }
      else this.errorMessage = 'Failed to publish offer.';
      this.cdr.detectChanges();
    });
  }
}
