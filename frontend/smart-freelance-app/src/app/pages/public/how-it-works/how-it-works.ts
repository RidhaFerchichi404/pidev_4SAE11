import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Card } from '../../../shared/components/card/card';
import { Button } from '../../../shared/components/button/button';

@Component({
  selector: 'app-how-it-works',
  imports: [RouterLink, Card, Button],
  templateUrl: './how-it-works.html',
  styleUrl: './how-it-works.scss',
  standalone: true,
})
export class HowItWorks {}
