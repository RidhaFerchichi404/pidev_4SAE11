import { Component, input, output, signal, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { ProjectFeed } from '../../../models/project-feed';

@Component({
  selector: 'app-project-feed-card',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './project-feed-card.component.html',
  styleUrl: './project-feed-card.component.scss',
})
export class ProjectFeedCard {
  // ── Signal-based inputs (Angular 17+) ────────────────────────────────────
  project   = input.required<ProjectFeed>();
  animDelay = input<string>('0ms');

  // ── Outputs ──────────────────────────────────────────────────────────────
  apply   = output<ProjectFeed>();
  save    = output<ProjectFeed>();
  message = output<ProjectFeed>();

  // ── Local reactive state ─────────────────────────────────────────────────
  isSaved = signal(false);

  readonly stars = [1, 2, 3, 4, 5];

  // Derived signals
  visibleSkills = computed(() => this.project().skills.slice(0, 5));
  extraSkills   = computed(() => Math.max(0, this.project().skills.length - 5));
  rating        = computed(() => this.project().client.rating);
  ratingFloor   = computed(() => Math.floor(this.rating()));

  private router = inject(Router);

  onApply(e: Event): void {
    e.stopPropagation();
    this.apply.emit(this.project());
    this.router.navigate(['/dashboard/my-projects', this.project().id, 'show']);
  }

  onSave(e: Event): void {
    e.stopPropagation();
    this.isSaved.update(v => !v);
    this.save.emit(this.project());
  }

  onMessage(e: Event): void {
    e.stopPropagation();
    this.message.emit(this.project());
    this.router.navigate(['/dashboard/messages']);
  }

  onCardClick(): void {
    this.router.navigate(['/dashboard/my-projects', this.project().id, 'show']);
  }

  onAvatarError(event: Event): void {
    const seed = encodeURIComponent(this.project().client.name);
    (event.target as HTMLImageElement).src =
      `https://api.dicebear.com/7.x/initials/svg?seed=${seed}`;
  }
}
