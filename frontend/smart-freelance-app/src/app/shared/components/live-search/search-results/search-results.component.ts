import { Component, input, output, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { SearchResult } from '../../../models/project-feed';

@Component({
  selector: 'app-search-results',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './search-results.component.html',
  styleUrl: './search-results.component.scss',
})
export class SearchResults {
  // ── Inputs ───────────────────────────────────────────────────────────────
  results     = input.required<SearchResult[]>();
  activeTab   = input.required<'projects' | 'categories'>();
  activeIndex = input<number>(-1);
  isLoading   = input<boolean>(false);
  query       = input<string>('');

  // ── Outputs ──────────────────────────────────────────────────────────────
  selectResult = output<SearchResult>();
  tabChange    = output<'projects' | 'categories'>();

  // ── Derived ──────────────────────────────────────────────────────────────
  projectResults  = computed(() => this.results().filter(r => r.type === 'project'));
  categoryResults = computed(() => this.results().filter(r => r.type === 'category'));
  activeList      = computed(() =>
    this.activeTab() === 'projects' ? this.projectResults() : this.categoryResults()
  );

  onSelect(result: SearchResult): void {
    this.selectResult.emit(result);
  }

  setTab(tab: 'projects' | 'categories'): void {
    this.tabChange.emit(tab);
  }

  isActive(index: number): boolean {
    return this.activeIndex() === index;
  }
}
