import { Component } from '@angular/core';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-admin-dashboard',
  imports: [Card],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
  standalone: true,
})
export class AdminDashboard {}
