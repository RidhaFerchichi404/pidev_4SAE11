import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ContractService, Contract, ContractStatus } from '../../../core/services/contract.service';
import { UserService } from '../../../core/services/user.service';

type FilterTab = 'ALL' | ContractStatus;

interface ContractForm {
  clientId: number;
  freelancerId: number;
  title: string;
  description: string;
  terms: string;
  amount: number;
  startDate: string;
  endDate: string;
}

@Component({
  selector: 'app-my-contracts',
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './my-contracts.html',
  styleUrl: './my-contracts.scss',
  standalone: true,
})
export class MyContracts implements OnInit {
  contracts: Contract[] = [];
  isLoading = true;
  errorMsg: string | null = null;

  activeFilter: FilterTab = 'ALL';
  readonly filterTabs: FilterTab[] = [
    'ALL', 'DRAFT', 'PENDING_SIGNATURE', 'ACTIVE', 'IN_CONFLICT', 'COMPLETED', 'CANCELLED'
  ];
  readonly skeletonItems = [1, 2, 3];

  // ── Create modal ──────────────────────────────────────────────
  showCreateModal = false;
  isSubmitting = false;
  createError: string | null = null;
  // Plain default — auth is not injected yet at field-initializer time
  form: ContractForm = { clientId: 0, freelancerId: 0, title: '', description: '', terms: '', amount: 0, startDate: '', endDate: '' };

  // Freelancer email lookup
  freelancerEmail = '';
  freelancerName: string | null = null;
  isLookingUp = false;
  lookupError: string | null = null;

  // Field-level validation errors (key → message)
  fieldErrors: Record<string, string> = {};

  constructor(
    public auth: AuthService,
    private contractSvc: ContractService,
    private userSvc: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() { this.loadContracts(); }

  // ── Load list ─────────────────────────────────────────────────

  loadContracts() {
    const userId = this.auth.getUserId();
    if (!userId) { this.isLoading = false; return; }

    const obs = this.auth.isClient()
      ? this.contractSvc.getByClient(userId)
      : this.contractSvc.getByFreelancer(userId);

    obs.subscribe({
      next: (list) => {
        this.contracts = Array.isArray(list) ? list : [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Could not load contracts. Make sure the Contract service is running on port 8083.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get filtered(): Contract[] {
    if (this.activeFilter === 'ALL') return this.contracts;
    return this.contracts.filter(c => c.status === this.activeFilter);
  }

  countByStatus(s: FilterTab): number {
    if (s === 'ALL') return this.contracts.length;
    return this.contracts.filter(c => c.status === s).length;
  }

  setFilter(f: FilterTab) { this.activeFilter = f; }

  viewDetail(id: number | undefined) {
    if (id) this.router.navigate(['/dashboard/my-contracts', id]);
  }

  // ── Create modal ──────────────────────────────────────────────

  openCreate() {
    const userId = this.auth.getUserId() || 0;
    this.form = { clientId: userId, freelancerId: 0, title: '', description: '', terms: '', amount: 0, startDate: '', endDate: '' };
    this.createError = null;
    this.fieldErrors = {};
    this.freelancerEmail = '';
    this.freelancerName = null;
    this.lookupError = null;
    this.showCreateModal = true;
  }

  closeCreate() { this.showCreateModal = false; }

  // ── Freelancer lookup by email ────────────────────────────────

  lookupFreelancer() {
    const email = this.freelancerEmail.trim();
    if (!email) { this.lookupError = 'Enter an email address.'; return; }

    this.isLookingUp = true;
    this.lookupError = null;
    this.freelancerName = null;
    this.form.freelancerId = 0;

    this.userSvc.getByEmail(email).subscribe({
      next: (user) => {
        this.isLookingUp = false;
        if (!user) {
          this.lookupError = 'No account found with this email.';
        } else if (user.role !== 'FREELANCER') {
          this.lookupError = `This account is registered as "${user.role}", not a Freelancer.`;
        } else {
          this.freelancerName = `${user.firstName} ${user.lastName}`;
          this.form.freelancerId = user.id;
          delete this.fieldErrors['freelancer'];
          this.lookupError = null;
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLookingUp = false;
        this.lookupError = 'Lookup failed. Check your connection.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Validation ────────────────────────────────────────────────

  private validate(): boolean {
    this.fieldErrors = {};

    // Title: required, starts with uppercase, ≥ 5 chars
    const title = this.form.title.trim();
    if (!title) {
      this.fieldErrors['title'] = 'Title is required.';
    } else if (!/^[A-Z]/.test(title)) {
      this.fieldErrors['title'] = 'Title must start with an uppercase letter (e.g. "Website Redesign").';
    } else if (title.length < 5) {
      this.fieldErrors['title'] = `Title must be at least 5 characters (currently ${title.length}).`;
    }

    // Terms: required, ≥ 20 chars
    const terms = this.form.terms.trim();
    if (!terms) {
      this.fieldErrors['terms'] = 'Terms & Conditions are required.';
    } else if (terms.length < 20) {
      this.fieldErrors['terms'] = `Terms must be at least 20 characters (currently ${terms.length}).`;
    }

    // Amount
    if (!this.form.amount || this.form.amount <= 0) {
      this.fieldErrors['amount'] = 'Budget must be greater than 0.';
    }

    // Freelancer resolved
    if (!this.form.freelancerId || this.form.freelancerId <= 0) {
      this.fieldErrors['freelancer'] = 'Use the lookup to find and confirm the freelancer.';
    }

    // Start date: required, today or future
    if (!this.form.startDate) {
      this.fieldErrors['startDate'] = 'Start date is required.';
    } else {
      const start = new Date(this.form.startDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (start < today) {
        this.fieldErrors['startDate'] = 'Start date cannot be in the past.';
      }
    }

    // End date: required, must be strictly after start
    if (!this.form.endDate) {
      this.fieldErrors['endDate'] = 'End date is required.';
    } else if (this.form.startDate && this.form.endDate <= this.form.startDate) {
      this.fieldErrors['endDate'] = 'End date must be after the start date.';
    }

    return Object.keys(this.fieldErrors).length === 0;
  }

  submitCreate() {
    if (!this.validate()) return;

    this.isSubmitting = true;
    this.createError = null;
    this.contractSvc.create(this.form).subscribe({
      next: (c) => {
        this.isSubmitting = false;
        this.showCreateModal = false;
        this.contracts.unshift(c);
        this.cdr.detectChanges();
        if (c.id) this.router.navigate(['/dashboard/my-contracts', c.id]);
      },
      error: () => {
        this.isSubmitting = false;
        this.createError = 'Failed to create contract. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────

  getStatusLabel(s?: ContractStatus): string {
    const map: Record<ContractStatus, string> = {
      DRAFT: 'Draft',
      PENDING_SIGNATURE: 'Pending Signature',
      ACTIVE: 'Active',
      IN_CONFLICT: 'In Conflict',
      COMPLETED: 'Completed',
      CANCELLED: 'Cancelled',
    };
    return s ? (map[s] ?? s) : '—';
  }

  getTabLabel(f: FilterTab): string {
    return f === 'ALL' ? 'All' : this.getStatusLabel(f as ContractStatus);
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  /** ISO today string for min date on date inputs */
  get todayIso(): string {
    return new Date().toISOString().split('T')[0];
  }
}
