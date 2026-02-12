import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-badge',
  imports: [],
  templateUrl: './badge.html',
  styleUrl: './badge.scss',
  standalone: true,
})
export class Badge {
  @Input() variant: 'success' | 'warning' | 'error' | 'info' | 'default' = 'default';
}
