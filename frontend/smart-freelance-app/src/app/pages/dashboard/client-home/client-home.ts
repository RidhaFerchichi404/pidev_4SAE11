import { Component, OnInit, signal, computed, inject, ChangeDetectorRef } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, Project, ClientSegment, SegmentationOverview } from '../../../core/services/project.service';
import { UserService } from '../../../core/services/user.service';
import { ContractService } from '../../../core/services/contract.service';
import {
  GamificationService,
  GamificationUserLevel,
  isTopFreelancerFlag,
} from '../../../core/services/gamification.service';
import { ProjectsFeed } from '../../../shared/components/projects-feed/projects-feed.component';
import { ProjectFeed } from '../../../shared/models/project-feed';
import { Card } from '../../../shared/components/card/card';

export type SortOption = 'newest' | 'oldest' | 'budget-high' | 'budget-low';

@Component({
  selector: 'app-client-home',
  standalone: true,
  imports: [ProjectsFeed, RouterLink, CommonModule, FormsModule],
  templateUrl: './client-home.html',
  styleUrl: './client-home.scss',
})
export class ClientHome implements OnInit {
  private projectService = inject(ProjectService);
  private auth = inject(AuthService);
  private us = inject(UserService);
  private contractService = inject(ContractService);
  private gamificationService = inject(GamificationService);
  private cdr = inject(ChangeDetectorRef);

  displayName = '';
  myProjectsCount = 0;
  activeContractsCount = 0;
  isLoadingStats = true;
  gamificationLevel: GamificationUserLevel | null = null;
  clientSegment: ClientSegment | null = null;
  segmentationOverview: SegmentationOverview | null = null;

  // Feed state
  private allProjects = signal<ProjectFeed[]>([]);
  searchTerm = '';
  sortOption: SortOption = 'newest';
  currentPage = signal(1);
  readonly pageSize = 6;
  isFetching = signal(true);

  filteredProjects = computed(() => {
    const list = this.allProjects();
    const q = (this.searchTerm || '').trim().toLowerCase();
    if (!q) return list;
    return list.filter(
      (p) =>
        p.title.toLowerCase().includes(q) ||
        (p.description || '').toLowerCase().includes(q) ||
        (p.category || '').toLowerCase().includes(q) ||
        p.skills.some((s) => s.toLowerCase().includes(q))
    );
  });

  sortedProjects = computed(() => {
    const list = [...this.filteredProjects()];
    const toMs = (p: ProjectFeed) => new Date(p.createdAt || 0).getTime();
    switch (this.sortOption) {
      case 'newest': return list.sort((a, b) => toMs(b) - toMs(a));
      case 'oldest': return list.sort((a, b) => toMs(a) - toMs(b));
      case 'budget-high': return list.sort((a, b) => b.budget - a.budget);
      case 'budget-low': return list.sort((a, b) => a.budget - b.budget);
      default: return list;
    }
  });

  totalCount = computed(() => this.sortedProjects().length);
  totalPages = computed(() => Math.max(1, Math.ceil(this.totalCount() / this.pageSize)));
  startIndex = computed(() => (this.currentPage() - 1) * this.pageSize);
  endIndex = computed(() => Math.min(this.startIndex() + this.pageSize, this.totalCount()));
  displayedProjects = computed(() => this.sortedProjects().slice(this.startIndex(), this.endIndex()));

  ngOnInit(): void {
    this.displayName = this.auth.getDisplayName() || 'Client';
    const userId = this.auth.getUserId();
    const email = this.auth.getPreferredUsername();

    if (userId) {
      this.loadClientStats(userId);
    }

    this.loadAllProjects();
  }

  private loadClientStats(clientId: number): void {
    this.isLoadingStats = true;
    forkJoin({
      projects: this.projectService.getByClientId(clientId).pipe(catchError(() => of([]))),
      contracts: this.contractService.getByClient(clientId).pipe(catchError(() => of([]))),
      gamification: this.gamificationService.getUserLevel(clientId).pipe(catchError(() => of(null))),
      segment: this.projectService.getClientSegment(clientId).pipe(catchError(() => of(null))),
      segmentationOverview: this.projectService.getSegmentationOverview().pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ projects, contracts, gamification, segment, segmentationOverview }) => {
        this.myProjectsCount = projects?.length ?? 0;
        const active = contracts?.filter(c => c.status === 'ACTIVE' || c.status === 'PENDING_SIGNATURE') ?? [];
        this.activeContractsCount = active.length;
        this.gamificationLevel = gamification;
        this.clientSegment = segment;
        this.segmentationOverview = segmentationOverview;
        this.isLoadingStats = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingStats = false;
        this.clientSegment = null;
        this.segmentationOverview = null;
        this.cdr.detectChanges();
      },
    });
  }

  private loadAllProjects(): void {
    const isAdmin = this.auth.getUserRole() === 'ADMIN';
    forkJoin({
      projects: this.projectService.getAllProjects(),
      users: isAdmin ? this.us.getAll().pipe(catchError(() => of([]))) : of([]),
    }).subscribe({
      next: ({ projects, users }) => {
        const userMap = new Map<number, { firstName: string; lastName: string; avatarUrl?: string }>();
        (users ?? []).forEach((u) => userMap.set(u.id, { firstName: u.firstName, lastName: u.lastName, avatarUrl: u.avatarUrl }));
        const open = (projects ?? []).filter(p => !p.status || p.status === 'OPEN');
        this.allProjects.set(open.map((p, i) => this.toFeed(p, i, userMap)));
        this.isFetching.set(false);
      },
      error: () => this.isFetching.set(false),
    });
  }

  private toFeed(p: Project, index: number, userMap: Map<number, { firstName: string; lastName: string; avatarUrl?: string }>): ProjectFeed {
    const owner = p.clientId != null ? userMap.get(p.clientId) : undefined;
    const fullName = owner ? `${owner.firstName} ${owner.lastName}`.trim() || 'Project Owner' : 'Project Owner';
    const avatarUrl = owner?.avatarUrl;
    return {
      id: String(p.id ?? index),
      title: p.title,
      budget: p.budget ?? 0,
      client: {
        name: fullName,
        avatar: avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${p.clientId ?? index}`,
        rating: Number.parseFloat((4.2 + Math.random() * 0.8).toFixed(1)),
      },
      description: p.description,
      skills: this.parseSkills(p.skills),
      postedAgo: this.timeAgo(p.createdAt),
      createdAt: p.createdAt,
      category: p.category,
    };
  }

  onSearchChange(): void { this.currentPage.set(1); }
  onSortChange(): void { this.currentPage.set(1); }
  goToPage(page: number): void {
    const total = this.totalPages();
    if (page >= 1 && page <= total) this.currentPage.set(page);
  }

  private parseSkills(s: unknown): string[] {
    if (!s) return [];
    if (Array.isArray(s)) {
      return s.map((x) => (typeof x === 'object' && x && 'name' in x ? (x as { name: string }).name : String(x))).filter(Boolean);
    }
    return String(s).split(',').map(x => x.trim()).filter(Boolean);
  }

  gamificationDisplayLevel(): number {
    const l = this.gamificationLevel?.level;
    if (l != null && l > 0) return l;
    const xp = this.gamificationLevel?.xp ?? 0;
    return Math.floor(xp / 100) + 1;
  }

  gamificationTopFreelancer(): boolean {
    return isTopFreelancerFlag(this.gamificationLevel);
  }

  segmentationSummaryEntries(): Array<{ label: string; count: number }> {
    const summary = this.segmentationOverview?.summaryCounts ?? {};
    return Object.entries(summary)
      .map(([label, count]) => ({ label, count: Number(count) || 0 }))
      .sort((a, b) => b.count - a.count);
  }

  totalSegmentedClients(): number {
    return this.segmentationSummaryEntries().reduce((acc, item) => acc + item.count, 0);
  }

  segmentShare(count: number): number {
    const total = this.totalSegmentedClients();
    if (total <= 0) return 0;
    return (count / total) * 100;
  }

  confidencePercent(): number {
    return Math.round((this.clientSegment?.confidence ?? 0) * 100);
  }

  confidenceLabel(): string {
    const p = this.confidencePercent();
    if (p >= 65) return 'High confidence';
    if (p >= 35) return 'Medium confidence';
    return 'Low confidence';
  }

  clientSegmentInterpretation(): string {
    const label = (this.clientSegment?.segmentLabel ?? '').toLowerCase();
    if (label.includes('reliable')) {
      return 'Your projects usually show stable delivery patterns and lower execution friction.';
    }
    if (label.includes('needs support')) {
      return 'Your portfolio shows signs of delivery friction; tighter planning and risk tracking can improve outcomes.';
    }
    if (label.includes('improving')) {
      return 'Your execution trend is improving. Continue with consistent scope control and progress follow-up.';
    }
    if (label.includes('growth')) {
      return 'Your profile is in a growth phase with mixed patterns across projects.';
    }
    return 'This segment summarizes your recent project patterns to help prioritize actions.';
  }

  private timeAgo(dateStr?: string): string {
    if (!dateStr) return 'recently';
    const diff = Date.now() - new Date(dateStr).getTime();
    const h = Math.floor(diff / 3_600_000);
    const d = Math.floor(h / 24);
    if (d > 0) return `${d}d ago`;
    if (h > 0) return `${h}h ago`;
    return 'just now';
  }
}
