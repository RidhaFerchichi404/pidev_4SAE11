import {
  Component,
  input,
  output,
  AfterViewInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  inject,
} from '@angular/core';
import { ProjectFeedCard } from './project-feed-card/project-feed-card.component';
import { ProjectFeed } from '../../models/project-feed';
import { IntersectionObserverService } from '../../../core/services/intersection-observer.service';

@Component({
  selector: 'app-projects-feed',
  standalone: true,
  imports: [ProjectFeedCard],
  templateUrl: './projects-feed.component.html',
  styleUrl: './projects-feed.component.scss',
})
export class ProjectsFeed implements AfterViewInit, OnDestroy {
  // ── Inputs ───────────────────────────────────────────────────────────────
  projects  = input.required<ProjectFeed[]>();
  hasMore   = input<boolean>(true);
  isLoading = input<boolean>(false);
  pageSize  = input<number>(6);

  // ── Outputs ──────────────────────────────────────────────────────────────
  loadMore = output<void>();

  @ViewChild('sentinel') private sentinelRef!: ElementRef<HTMLDivElement>;

  private ioService = inject(IntersectionObserverService);
  private cleanup?: () => void;

  /**
   * Stagger delay: cards within each page-batch slide up with 0–300ms offset.
   * Uses modulo so only the newest batch animates in (not all previous cards).
   */
  getAnimDelay(index: number): string {
    const delay = (index % this.pageSize()) * 100;
    return `${Math.min(delay, 300)}ms`;
  }

  ngAfterViewInit(): void {
    if (!this.sentinelRef?.nativeElement) return;

    this.cleanup = this.ioService.observe(
      this.sentinelRef.nativeElement,
      (isIntersecting) => {
        if (isIntersecting && this.hasMore() && !this.isLoading()) {
          this.loadMore.emit();
        }
      },
      { rootMargin: '200px', threshold: 0 },
    );
  }

  ngOnDestroy(): void {
    this.cleanup?.();
  }
}
