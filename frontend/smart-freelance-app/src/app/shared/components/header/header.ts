import { Component, Input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { Button } from '../button/button';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterLink, Button],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  standalone: true,
})
export class Header {
  @Input() variant: 'public' | 'dashboard' | 'admin' = 'public';

  mobileMenuOpen = signal(false);
  userMenuOpen = signal(false);

  constructor(public auth: AuthService) {}

  toggleMobileMenu() {
    this.mobileMenuOpen.update(v => !v);
  }

  toggleUserMenu() {
    this.userMenuOpen.update(v => !v);
  }

  logout() {
    this.auth.logout();
    this.userMenuOpen.set(false);
  }
}
