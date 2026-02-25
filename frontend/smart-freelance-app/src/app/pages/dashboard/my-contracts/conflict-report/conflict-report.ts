import { Component, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContractService } from '../../../../core/services/contract.service';

const CONFLICT_REASONS = [
  'Payment Dispute',
  'Deliverable Not Met',
  'Missed Deadline',
  'Scope Change Disagreement',
  'Quality Issue',
  'Communication Breakdown',
  'Contract Violation',
  'Other',
] as const;

@Component({
  selector: 'app-conflict-report',
  imports: [CommonModule, FormsModule],
  templateUrl: './conflict-report.html',
  styleUrl: './conflict-report.scss',
  standalone: true,
})
export class ConflictReport {
  @Input() contractId!: number;
  @Input() raisedById!: number;

  @Output() conflictReported = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  readonly reasons = CONFLICT_REASONS;

  form = {
    reason: '',
    description: '',
    evidenceUrl: '',
  };

  isSubmitting = false;
  error: string | null = null;
  fieldErrors: Partial<Record<'reason' | 'description', string>> = {};

  constructor(
    private contractSvc: ContractService,
    private cdr: ChangeDetectorRef
  ) {}

  validate(): boolean {
    this.fieldErrors = {};
    if (!this.form.reason) this.fieldErrors['reason'] = 'Please select a reason.';
    if (!this.form.description || this.form.description.trim().length < 20) {
      this.fieldErrors['description'] = 'Please provide at least 20 characters of detail.';
    }
    return Object.keys(this.fieldErrors).length === 0;
  }

  submit() {
    if (!this.validate()) return;
    this.isSubmitting = true;
    this.error = null;

    this.contractSvc.reportConflict(this.contractId, {
      raisedById: this.raisedById,
      reason: this.form.reason,
      description: this.form.description.trim(),
      evidenceUrl: this.form.evidenceUrl.trim() || undefined,
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.conflictReported.emit();
      },
      error: () => {
        this.isSubmitting = false;
        this.error = 'Failed to submit conflict report. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  cancel() { this.cancelled.emit(); }
}
