import {
  Component,
  OnInit,
  signal,
  computed,
  HostListener,
  inject,
  DestroyRef,
} from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import {
  debounceTime,
  distinctUntilChanged,
  switchMap,
  tap,
  catchError,
} from 'rxjs/operators';
import { of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SearchResults } from './search-results/search-results.component';
import { ProjectService } from '../../../core/services/project.service';
import { SearchResult, FEED_CATEGORIES } from '../../models/project-feed';

@Component({
  selector: 'app-live-search',
  standalone: true,
  imports: [ReactiveFormsModule, SearchResults],
  templateUrl: './live-search.component.html',
  styleUrl: './live-search.component.scss',
})
export class LiveSearch implements OnInit {
  // ── Form control ─────────────────────────────────────────────────────────
  readonly searchControl = new FormControl<string>('');

  // ── Reactive state ───────────────────────────────────────────────────────
  results     = signal<SearchResult[]>([]);
  isLoading   = signal(false);
  isOpen      = signal(false);
  activeTab   = signal<'projects' | 'categories'>('projects');
  activeIndex = signal(-1);

  query = computed(() => this.searchControl.value ?? '');

  private projectService = inject(ProjectService);
  private router         = inject(Router);
  private destroyRef     = inject(DestroyRef);

  ngOnInit(): void {
    // RxJS pipeline: debounce → distinct → search
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap((q) => {
        if (q && q.trim().length > 0) {
          this.isLoading.set(true);
          this.isOpen.set(true);
        } else {
          this.results.set([]);
          this.isOpen.set(false);
          this.isLoading.set(false);
        }
      }),
      switchMap((q) =>
        q && q.trim().length > 0 ? this.search(q.trim()) : of<SearchResult[]>([]),
      ),
      catchError(() => of<SearchResult[]>([])),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((results) => {
      this.results.set(results);
      this.isLoading.set(false);
      this.activeIndex.set(-1);

      // Auto-switch tab: prefer projects; fall back to categories
      const hasProjects   = results.some(r => r.type === 'project');
      const hasCategories = results.some(r => r.type === 'category');
      if (!hasProjects && hasCategories) {
        this.activeTab.set('categories');
      } else {
        this.activeTab.set('projects');
      }
    });
  }

  // ── Search logic ─────────────────────────────────────────────────────────
  private search(query: string) {
    return this.projectService.getAllProjects().pipe(
      switchMap((projects) => {
        const q = query.toLowerCase();

        const projectResults: SearchResult[] = projects
          .filter((p) =>
            p.title.toLowerCase().includes(q) ||
            (p.description?.toLowerCase().includes(q) ?? false) ||
            (typeof p.skillsRequiered === 'string' &&
              p.skillsRequiered.toLowerCase().includes(q)),
          )
          .slice(0, 5)
          .map((p) => ({
            type: 'project' as const,
            id: String(p.id),
            title: p.title,
            budget: p.budget,
            skills: this.parseSkills(p.skillsRequiered),
          }));

        const categoryResults: SearchResult[] = FEED_CATEGORIES
          .filter((c) => c.name.toLowerCase().includes(q))
          .map((c) => ({
            type: 'category' as const,
            id: c.id,
            title: c.name,
            icon: c.icon,
            projectCount: projects.filter((p) => p.category === c.id).length,
          }));

        return of([...projectResults, ...categoryResults]);
      }),
      catchError(() => of<SearchResult[]>([])),
    );
  }

  private parseSkills(s: string | string[] | null | undefined): string[] {
    if (!s) return [];
    if (Array.isArray(s)) return s;
    return s.split(',').map((x) => x.trim()).filter(Boolean);
  }

  // ── Events ───────────────────────────────────────────────────────────────
  onFocus(): void {
    if (this.results().length > 0) this.isOpen.set(true);
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.results.set([]);
    this.close();
  }

  close(): void {
    this.isOpen.set(false);
    this.activeIndex.set(-1);
  }

  onSelectResult(result: SearchResult): void {
    this.clearSearch();
    if (result.type === 'project') {
      this.router.navigate(['/dashboard/my-projects', result.id, 'show']);
    } else {
      this.router.navigate(['/dashboard/browse-offers'], {
        queryParams: { category: result.id },
      });
    }
  }

  onTabChange(tab: 'projects' | 'categories'): void {
    this.activeTab.set(tab);
    this.activeIndex.set(-1);
  }

  // ── Keyboard navigation ──────────────────────────────────────────────────
  @HostListener('document:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    if (!this.isOpen()) return;

    const tabType = this.activeTab() === 'projects' ? 'project' : 'category';
    const list = this.results().filter(r => r.type === tabType);

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        this.activeIndex.update(i => Math.min(i + 1, list.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        this.activeIndex.update(i => Math.max(i - 1, -1));
        break;
      case 'Enter':
        if (this.activeIndex() >= 0 && list[this.activeIndex()]) {
          this.onSelectResult(list[this.activeIndex()]);
        }
        break;
      case 'Escape':
        this.close();
        break;
    }
  }

  // ── Outside-click close ──────────────────────────────────────────────────
  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent): void {
    if (!(e.target as HTMLElement).closest('.live-search')) {
      this.close();
    }
  }
}
