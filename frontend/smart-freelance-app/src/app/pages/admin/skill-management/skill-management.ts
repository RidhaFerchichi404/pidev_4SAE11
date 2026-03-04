import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortfolioService, Skill, Domain, DOMAIN_LABELS } from '../../../core/services/portfolio.service';

@Component({
  selector: 'app-admin-skill-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './skill-management.html',
  styleUrl: './skill-management.scss',
})
export class AdminSkillManagement implements OnInit {
  skills: Skill[] = [];
  loading = false;
  errorMessage: string | null = null;

  // Add modal
  showAddModal = false;
  addName = '';
  addUserId: number | null = null;
  addSelectedDomains = new Set<Domain>();
  showAddDomainDropdown = false;
  addFormErrors: Record<string, string> = {};
  saving = false;

  // Edit modal
  showEditModal = false;
  editSkill: Skill | null = null;
  editName = '';
  editUserId: number | null = null;
  editSelectedDomains = new Set<Domain>();
  showEditDomainDropdown = false;
  editFormErrors: Record<string, string> = {};
  updating = false;

  // Delete modal
  skillToDelete: Skill | null = null;
  deleting = false;

  // Helpers
  readonly availableDomains: Domain[] = Object.keys(DOMAIN_LABELS) as Domain[];
  readonly domainLabels = DOMAIN_LABELS;

  // Search / filter
  searchText = '';

  constructor(
    private portfolioService: PortfolioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadSkills();
  }

  loadSkills() {
    this.loading = true;
    this.errorMessage = null;
    this.portfolioService.getAllSkills().subscribe({
      next: (data) => {
        this.skills = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load skills. Is the Portfolio service running?';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Number of distinct users who have a skill with this name. */
  userCountBySkillName(name: string): number {
    return new Set(
      this.skills
        .filter(s => s.name.toLowerCase() === name.toLowerCase() && s.userId != null)
        .map(s => s.userId!)
    ).size;
  }

  get filteredSkills(): Skill[] {
    const q = this.searchText.trim().toLowerCase();
    if (!q) return this.skills;
    return this.skills.filter(s =>
      s.name.toLowerCase().includes(q) ||
      s.domains.some(d => this.domainLabels[d].toLowerCase().includes(q))
    );
  }

  // ── Domain dropdown helpers ────────────────────────────────

  toggleAddDomain(d: Domain) {
    if (this.addSelectedDomains.has(d)) this.addSelectedDomains.delete(d);
    else this.addSelectedDomains.add(d);
    if (this.addFormErrors['domains']) this.addFormErrors['domains'] = '';
  }

  toggleEditDomain(d: Domain) {
    if (this.editSelectedDomains.has(d)) this.editSelectedDomains.delete(d);
    else this.editSelectedDomains.add(d);
    if (this.editFormErrors['domains']) this.editFormErrors['domains'] = '';
  }

  domainsArray(set: Set<Domain>): Domain[] { return [...set]; }

  domainChipsLabel(set: Set<Domain>): string {
    if (set.size === 0) return '';
    const arr = [...set];
    const shown = arr.slice(0, 2).map(d => this.domainLabels[d]).join(', ');
    return arr.length > 2 ? `${shown} +${arr.length - 2}` : shown;
  }

  // ── Add ────────────────────────────────────────────────────

  openAdd() {
    this.addName = '';
    this.addUserId = null;
    this.addSelectedDomains = new Set();
    this.addFormErrors = {};
    this.showAddDomainDropdown = false;
    this.errorMessage = null;
    this.showAddModal = true;
  }

  private validateAdd(): boolean {
    this.addFormErrors = {};
    const name = this.addName.trim();
    if (!name) { this.addFormErrors['name'] = 'Skill name is required.'; }
    else if (name.length < 2) { this.addFormErrors['name'] = 'At least 2 characters.'; }
    else if (name.length > 50) { this.addFormErrors['name'] = 'Max 50 characters.'; }
    if (this.addSelectedDomains.size === 0) { this.addFormErrors['domains'] = 'Select at least one domain.'; }
    if (!this.addUserId || this.addUserId <= 0) { this.addFormErrors['userId'] = 'Valid user ID is required.'; }
    return Object.keys(this.addFormErrors).length === 0;
  }

  saveAdd() {
    if (!this.validateAdd()) return;
    this.saving = true;
    const skill: Skill = {
      name: this.addName.trim(),
      domains: [...this.addSelectedDomains],
      description: 'Added by admin',
      userId: this.addUserId!
    };
    this.portfolioService.createSkill(skill).subscribe({
      next: (created) => {
        this.skills = [created, ...this.skills];
        this.saving = false;
        this.showAddModal = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.saving = false;
        this.addFormErrors['name'] = err?.error?.message || 'Failed to create skill.';
        this.cdr.detectChanges();
      }
    });
  }

  closeAdd() { if (!this.saving) this.showAddModal = false; }

  // ── Edit ───────────────────────────────────────────────────

  openEdit(skill: Skill) {
    this.editSkill = skill;
    this.editName = skill.name;
    this.editUserId = skill.userId ?? null;
    this.editSelectedDomains = new Set(skill.domains);
    this.editFormErrors = {};
    this.showEditDomainDropdown = false;
    this.errorMessage = null;
    this.showEditModal = true;
  }

  private validateEdit(): boolean {
    this.editFormErrors = {};
    const name = this.editName.trim();
    if (!name) { this.editFormErrors['name'] = 'Skill name is required.'; }
    else if (name.length < 2) { this.editFormErrors['name'] = 'At least 2 characters.'; }
    else if (name.length > 50) { this.editFormErrors['name'] = 'Max 50 characters.'; }
    if (this.editSelectedDomains.size === 0) { this.editFormErrors['domains'] = 'Select at least one domain.'; }
    if (!this.editUserId || this.editUserId <= 0) { this.editFormErrors['userId'] = 'Valid user ID is required.'; }
    return Object.keys(this.editFormErrors).length === 0;
  }

  saveEdit() {
    if (!this.editSkill || !this.validateEdit()) return;
    this.updating = true;
    const updated: Skill = {
      ...this.editSkill,
      name: this.editName.trim(),
      domains: [...this.editSelectedDomains],
      userId: this.editUserId!
    };
    this.portfolioService.updateSkill(this.editSkill.id!, updated).subscribe({
      next: (result) => {
        const idx = this.skills.findIndex(s => s.id === this.editSkill!.id);
        if (idx !== -1) this.skills[idx] = result;
        this.updating = false;
        this.showEditModal = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.updating = false;
        this.editFormErrors['name'] = err?.error?.message || 'Failed to update skill.';
        this.cdr.detectChanges();
      }
    });
  }

  closeEdit() { if (!this.updating) this.showEditModal = false; }

  // ── Delete ─────────────────────────────────────────────────

  openDelete(skill: Skill) { this.skillToDelete = skill; }
  closeDelete() { if (!this.deleting) this.skillToDelete = null; }

  confirmDelete() {
    if (!this.skillToDelete) return;
    this.deleting = true;
    this.portfolioService.deleteSkill(this.skillToDelete.id!).subscribe({
      next: () => {
        this.skills = this.skills.filter(s => s.id !== this.skillToDelete!.id);
        this.deleting = false;
        this.skillToDelete = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete skill.';
        this.skillToDelete = null;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Helpers ────────────────────────────────────────────────

  domainsDisplay(skill: Skill): string {
    if (!skill.domains?.length) return '—';
    return skill.domains.map(d => this.domainLabels[d] ?? d).join(', ');
  }
}
