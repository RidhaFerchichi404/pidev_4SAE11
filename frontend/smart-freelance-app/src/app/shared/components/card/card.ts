import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-card',
  imports: [],
  templateUrl: './card.html',
  styleUrl: './card.scss',
  standalone: true,
})
export class Card {
  @Input() padding: 'sm' | 'md' | 'lg' = 'md';
  @Input() hoverable: boolean = false;
}
