import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  imports: [],
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.scss',
  standalone: true,
})
export class EmptyState {
  @Input() title: string = 'No data available';
  @Input() message: string = 'There is nothing to display at the moment.';
  @Input() icon: string = '📭';
}
