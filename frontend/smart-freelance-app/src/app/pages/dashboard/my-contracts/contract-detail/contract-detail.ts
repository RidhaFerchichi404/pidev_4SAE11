import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ContractService, Contract, ContractStatus, ContractConflict } from '../../../../core/services/contract.service';
import { UserService } from '../../../../core/services/user.service';
import { ConflictReport } from '../conflict-report/conflict-report';

@Component({
  selector: 'app-contract-detail',
  imports: [CommonModule, FormsModule, RouterLink, ConflictReport],
  templateUrl: './contract-detail.html',
  styleUrl: './contract-detail.scss',
  standalone: true,
})
export class ContractDetail implements OnInit {
  contract: Contract | null = null;
  conflicts: ContractConflict[] = [];
  isLoading = true;
  errorMsg: string | null = null;
  actionMsg: string | null = null;
  actionError: string | null = null;

  // Edit / propose changes
  showEditForm = false;
  editDraft: Partial<Contract> = {};
  isSavingEdit = false;

  // Signature
  showSignPad = false;
  signaturePreview: string | null = null;
  isSigningFile = false;

  // Conflict modal (child)
  showConflictModal = false;

  // Resolved party names
  clientName: string | null = null;
  freelancerName: string | null = null;

  constructor(
    public auth: AuthService,
    private contractSvc: ContractService,
    private userSvc: UserService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) { this.errorMsg = 'Invalid contract ID.'; this.isLoading = false; return; }
    this.loadContract(id);
  }

  loadContract(id: number) {
    this.isLoading = true;
    this.contractSvc.getById(id).subscribe({
      next: (c) => {
        this.contract = c;
        this.isLoading = false;
        this.loadConflicts(id);
        this.loadPartyNames(c);
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Contract not found or service unavailable.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadPartyNames(c: Contract) {
    if (c.clientId) {
      this.userSvc.getById(c.clientId).subscribe({
        next: (u) => {
          if (u) { this.clientName = `${u.firstName} ${u.lastName}`; this.cdr.detectChanges(); }
        },
        error: () => {}
      });
    }
    if (c.freelancerId) {
      this.userSvc.getById(c.freelancerId).subscribe({
        next: (u) => {
          if (u) { this.freelancerName = `${u.firstName} ${u.lastName}`; this.cdr.detectChanges(); }
        },
        error: () => {}
      });
    }
  }

  loadConflicts(contractId: number) {
    this.contractSvc.getConflicts(contractId).subscribe({
      next: (list) => { this.conflicts = Array.isArray(list) ? list : []; this.cdr.detectChanges(); },
      error: () => { this.conflicts = []; }
    });
  }

  // ── Getters ──────────────────────────────────────────────────────

  get myRole(): 'CLIENT' | 'FREELANCER' | null {
    if (this.auth.isClient()) return 'CLIENT';
    if (this.auth.isFreelancer()) return 'FREELANCER';
    return null;
  }

  get canEdit(): boolean {
    return this.contract?.status === 'DRAFT' && this.auth.isClient();
  }

  get canSubmitForSignature(): boolean {
    return this.contract?.status === 'DRAFT' && this.auth.isClient();
  }

  get canProposeChanges(): boolean {
    return this.contract?.status === 'PENDING_SIGNATURE' && this.auth.isFreelancer();
  }

  get canSign(): boolean {
    const s = this.contract?.status;
    if (s !== 'PENDING_SIGNATURE') return false;
    if (this.auth.isClient() && !this.contract?.clientSignatureUrl) return true;
    if (this.auth.isFreelancer() && !this.contract?.freelancerSignatureUrl) return true;
    return false;
  }

  get canReject(): boolean {
    return this.contract?.status === 'PENDING_SIGNATURE' && this.auth.isFreelancer();
  }

  get canComplete(): boolean {
    return this.contract?.status === 'ACTIVE' && this.auth.isClient();
  }

  get canReportConflict(): boolean {
    const s = this.contract?.status;
    return s === 'ACTIVE' || s === 'IN_CONFLICT';
  }

  // ── Conflict grouping ─────────────────────────────────────────────
  get clientConflicts(): ContractConflict[] {
    return this.conflicts.filter(c => c.raisedById === this.contract?.clientId);
  }

  get freelancerConflicts(): ContractConflict[] {
    return this.conflicts.filter(c => c.raisedById === this.contract?.freelancerId);
  }

  get isSigned(): boolean {
    return !!(this.contract?.clientSignatureUrl && this.contract?.freelancerSignatureUrl);
  }

  // ── Actions ──────────────────────────────────────────────────────

  submitForSignature() {
    if (!this.contract?.id) return;
    this.contractSvc.updateStatus(this.contract.id, 'PENDING_SIGNATURE').subscribe({
      next: (c) => { this.contract = c; this.actionMsg = 'Sent for signature!'; this.cdr.detectChanges(); },
      error: () => { this.actionError = 'Failed to submit for signature.'; this.cdr.detectChanges(); }
    });
  }

  rejectContract() {
    if (!this.contract?.id) return;
    this.contractSvc.updateStatus(this.contract.id, 'DRAFT').subscribe({
      next: (c) => { this.contract = c; this.actionMsg = 'Contract sent back for revision.'; this.cdr.detectChanges(); },
      error: () => { this.actionError = 'Action failed.'; this.cdr.detectChanges(); }
    });
  }

  completeContract() {
    if (!this.contract?.id) return;
    this.contractSvc.updateStatus(this.contract.id, 'COMPLETED').subscribe({
      next: (c) => { this.contract = c; this.actionMsg = 'Contract marked as completed!'; this.cdr.detectChanges(); },
      error: () => { this.actionError = 'Failed to complete contract.'; this.cdr.detectChanges(); }
    });
  }

  // ── Edit / Propose Changes ────────────────────────────────────────

  openEdit() {
    if (!this.contract) return;
    this.editDraft = {
      title: this.contract.title,
      description: this.contract.description,
      terms: this.contract.terms,
      amount: this.contract.amount,
      startDate: this.contract.startDate,
      endDate: this.contract.endDate,
    };
    this.showEditForm = true;
  }

  cancelEdit() { this.showEditForm = false; }

  saveEdit() {
    if (!this.contract?.id) return;
    this.isSavingEdit = true;
    const payload: Partial<Contract> = {
      ...this.editDraft,
      status: 'DRAFT', // propose changes resets to DRAFT
    };
    this.contractSvc.update(this.contract.id, payload).subscribe({
      next: (c) => {
        this.contract = c;
        this.showEditForm = false;
        this.isSavingEdit = false;
        this.actionMsg = 'Changes saved. Contract is back in Draft.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.isSavingEdit = false;
        this.actionError = 'Failed to save changes.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Signature ────────────────────────────────────────────────────

  openSignPad() { this.showSignPad = true; this.signaturePreview = null; }
  closeSignPad() { this.showSignPad = false; this.signaturePreview = null; }

  onSignatureFile(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      this.signaturePreview = reader.result as string;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);
  }

  submitSignature() {
    if (!this.contract?.id || !this.signaturePreview || !this.myRole) return;
    this.isSigningFile = true;
    this.contractSvc.sign(this.contract.id, this.myRole, this.signaturePreview).subscribe({
      next: (c) => {
        this.contract = c;
        this.showSignPad = false;
        this.signaturePreview = null;
        this.isSigningFile = false;
        this.actionMsg = c.status === 'ACTIVE'
          ? '✅ Both signatures collected — contract is now Active!'
          : '✅ Signature submitted! Waiting for the other party.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.isSigningFile = false;
        this.actionError = 'Failed to submit signature.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Conflict modal ────────────────────────────────────────────────

  openConflictModal() { this.showConflictModal = true; }
  closeConflictModal() { this.showConflictModal = false; }

  onConflictReported() {
    this.showConflictModal = false;
    if (this.contract?.id) {
      this.loadConflicts(this.contract.id);
      // Also flag the contract as IN_CONFLICT
      this.contractSvc.updateStatus(this.contract.id, 'IN_CONFLICT').subscribe({
        next: (c) => { this.contract = c; this.cdr.detectChanges(); }
      });
    }
    this.actionMsg = 'Conflict reported successfully.';
  }

  // ── Helpers ───────────────────────────────────────────────────────

  getStatusLabel(s?: ContractStatus): string {
    const m: Record<ContractStatus, string> = {
      DRAFT: 'Draft', PENDING_SIGNATURE: 'Pending Signature',
      ACTIVE: 'Active', IN_CONFLICT: 'In Conflict',
      COMPLETED: 'Completed', CANCELLED: 'Cancelled',
    };
    return s ? (m[s] ?? s) : '—';
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  dismissMsg() { this.actionMsg = null; this.actionError = null; }
}
