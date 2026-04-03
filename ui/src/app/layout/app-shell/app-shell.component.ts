import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NzLayoutModule,
    NzButtonModule,
    NzTagModule
  ],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css'
})
export class AppShellComponent {
  private readonly authService = inject(AuthService);
  private readonly roleMeta: Record<string, { label: string; color: string }> = {
    ADMIN: { label: 'Administrator', color: 'geekblue' },
    PREMIUM: { label: 'Premium', color: 'blue' },
    USER: { label: 'Utilizator', color: 'cyan' }
  };

  readonly user = this.authService.user;
  readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));
  readonly displayRoles = computed(() => {
    const roles = (this.user()?.roles ?? [])
      .map((role) => role.trim().toUpperCase())
      .filter(Boolean);

    if (roles.includes('ADMIN')) {
      return ['ADMIN'];
    }

    return Array.from(new Set(roles));
  });
  readonly initials = computed(() => {
    const raw = this.user()?.username?.trim();
    if (!raw) {
      return 'U';
    }

    return raw
      .split(/[\s._-]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('') || 'U';
  });

  roleLabel(role: string): string {
    const normalized = role.trim().toUpperCase();
    return this.roleMeta[normalized]?.label ?? normalized;
  }

  roleColor(role: string): string {
    const normalized = role.trim().toUpperCase();
    return this.roleMeta[normalized]?.color ?? 'geekblue';
  }

  logout(): void {
    this.authService.logout();
  }
}

