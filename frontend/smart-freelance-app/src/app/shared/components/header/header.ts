import { Component, Input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { Button } from '../button/button';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterLink, Button],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  standalone: true,
})
export class Header implements OnInit {
  @Input() variant: 'public' | 'dashboard' | 'admin' = 'public';

  mobileMenuOpen = signal(false);
  userMenuOpen = signal(false);
  avatarUrl = signal<string | null>(null);

  constructor(
    public auth: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      const email = this.auth.getPreferredUsername();
      if (email) {
        this.userService.getByEmail(email).subscribe((user) => {
          const url = user?.avatarUrl?.trim() || null;
          setTimeout(() => this.avatarUrl.set(url), 0);
        });
      }
    }
  }

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

  onAvatarError(): void {
    this.avatarUrl.set(null);
  }
}
