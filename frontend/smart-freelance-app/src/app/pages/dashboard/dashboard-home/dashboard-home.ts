import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-dashboard-home',
  imports: [Card],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
  standalone: true,
})
export class DashboardHome {
  constructor(public auth: AuthService) {}
}
