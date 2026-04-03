import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { catchError, forkJoin, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Portfolio, Stock } from '../../core/models';

@Component({
  selector: 'app-user-page',
  standalone: true,
  imports: [
    CommonModule,
    NzCardModule,
    NzButtonModule,
    NzTagModule,
    NzAvatarModule,
    NzSkeletonModule,
    NzProgressModule
  ],
  templateUrl: './user-page.component.html',
  styleUrl: './user-page.component.css'
})
export class UserPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly authService = inject(AuthService);
  private readonly message = inject(NzMessageService);
  private readonly roleMeta: Record<string, { label: string; color: string }> = {
    ADMIN: { label: 'Administrator', color: 'geekblue' },
    PREMIUM: { label: 'Premium', color: 'blue' },
    USER: { label: 'Utilizator', color: 'cyan' }
  };

  readonly user = this.authService.user;
  readonly loading = signal(true);
  readonly portfolios = signal<Portfolio[]>([]);
  readonly stocks = signal<Stock[]>([]);

  readonly initials = computed(() => {
    const username = this.user()?.username?.trim();
    if (!username) {
      return 'U';
    }

    return username
      .split(/[\s._-]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((chunk) => chunk.charAt(0).toUpperCase())
      .join('') || 'U';
  });

  readonly watchlistSize = computed(() => this.stocks().length);
  readonly totalPortfolios = computed(() => this.portfolios().length);
  readonly totalHoldings = computed(() =>
    this.portfolios().reduce((total, portfolio) => total + portfolio.holdings.length, 0)
  );
  readonly investedCapital = computed(() =>
    this.portfolios().reduce(
      (portfolioTotal, portfolio) =>
        portfolioTotal
        + portfolio.holdings.reduce((holdingTotal, holding) => holdingTotal + holding.quantity * holding.purchasePrice, 0),
      0
    )
  );
  readonly averagePosition = computed(() => {
    const positions = this.totalHoldings();
    if (positions === 0) {
      return 0;
    }

    return this.investedCapital() / positions;
  });

  readonly profileScore = computed(() => {
    let score = 40;

    if (this.user()?.email) {
      score += 20;
    }

    if ((this.user()?.roles.length ?? 0) > 0) {
      score += 15;
    }

    if (this.totalPortfolios() > 0) {
      score += 15;
    }

    if (this.totalHoldings() > 0) {
      score += 10;
    }

    return Math.min(score, 100);
  });

  ngOnInit(): void {
    this.loadSnapshot(false);
  }

  reload(): void {
    this.loadSnapshot(true);
  }

  roleLabel(role: string): string {
    const normalized = role.trim().toUpperCase();
    return this.roleMeta[normalized]?.label ?? normalized;
  }

  roleColor(role: string): string {
    const normalized = role.trim().toUpperCase();
    return this.roleMeta[normalized]?.color ?? 'geekblue';
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('ro-RO', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  }

  private loadSnapshot(showSuccess: boolean): void {
    this.loading.set(true);

    forkJoin({
      stocks: this.api.getStocks().pipe(catchError(() => of([] as Stock[]))),
      portfolios: this.api.getMyPortfolios().pipe(catchError(() => of([] as Portfolio[])))
    }).subscribe({
      next: ({ stocks, portfolios }) => {
        this.stocks.set(stocks);
        this.portfolios.set(portfolios);

        if (showSuccess) {
          this.message.success('Profilul a fost actualizat.');
        }
      },
      complete: () => this.loading.set(false)
    });
  }
}

