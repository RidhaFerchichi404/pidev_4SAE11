import { Component } from '@angular/core';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-about',
  imports: [Card],
  templateUrl: './about.html',
  styleUrl: './about.scss',
  standalone: true,
})
export class About {}
