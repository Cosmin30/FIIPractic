import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { catchError, forkJoin, of } from 'rxjs';
import { NzMessageService } from 'ng-zorro-antd/message';
import { PageHelpComponent } from '../../components/page-help/page-help.component';
import { ApiService } from '../../core/api.service';
import { AdminDeletedPortfolioInsight, AdminDiversifiedPortfolioInsight, Portfolio, UserInfo } from '../../core/models';

import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-portfolios-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule, 
    NzCardModule,
    NzSwitchModule,
    NzTableModule,
    NzTagModule,
    NzButtonModule,
    NzFormModule,
    NzInputModule,
    PageHelpComponent
  ],
  templateUrl: './admin-portfolios-page.component.html',
  styleUrl: './admin-portfolios-page.component.css'
})
export class AdminPortfoliosPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly message = inject(NzMessageService);
  private readonly fb = inject(FormBuilder);

  readonly currentUser = signal<UserInfo | null>(null);
  readonly portfolios = signal<Portfolio[]>([]);
  readonly loading = signal(false);
  readonly insightsLoading = signal(false);
  readonly diversifiedInsights = signal<AdminDiversifiedPortfolioInsight[]>([]);
  readonly deletedInsights = signal<AdminDeletedPortfolioInsight[]>([]);
  readonly userAliasById = computed(() => {
    const currentUserId = this.currentUser()?.userId;
    const ids = new Set<string>();

    this.diversifiedInsights().forEach((row) => {
      if (row.userId && row.userId !== currentUserId) {
        ids.add(row.userId);
      }
    });

    this.deletedInsights().forEach((row) => {
      if (row.userId && row.userId !== currentUserId) {
        ids.add(row.userId);
      }

      if (row.deletedBy && row.deletedBy !== currentUserId) {
        ids.add(row.deletedBy);
      }
    });

    const aliases = new Map<string, string>();
    Array.from(ids)
      .sort()
      .forEach((id, index) => aliases.set(id, `Utilizator ${index + 1}`));

    return aliases;
  });
  readonly totalPortfolios = computed(() => this.portfolios().length);
  readonly deletedCount = computed(() => this.portfolios().filter((portfolio) => portfolio.deleted).length);
  readonly activeCount = computed(() => this.totalPortfolios() - this.deletedCount());
  readonly helpIntro = 'Pagina Admin este pentru supervizarea tuturor portofoliilor din platforma.';
  readonly helpSteps = [
    'Actualizeaza datele din butoanele de sus: portofolii si insights.',
    'Activeaza Include portofolii sterse pentru audit complet.',
    'Din tabel poti sterge, reimprospata cotatii si analiza evaluarea fiecarui portofoliu.'
  ];
  readonly helpTips = [
    'In tabele, utilizatorii sunt afisati cu alias pentru protectia identitatii.',
    'Este recomandat sa verifici evaluarea inainte de operatii administrative majore.'
  ];

  includeDeleted = false;

  createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    description: ['']
  });

  showValuation: Record<number, boolean> = {};
  valuationData: Record<number, any> = {};

  ngOnInit(): void {
    this.loadCurrentUser();
    this.load();
    this.loadInsights();
  }

  userDisplay(userId: string | null | undefined): string {
    if (!userId) {
      return '-';
    }

    const currentUser = this.currentUser();
    if (currentUser?.userId === userId) {
      return currentUser.username?.trim() || 'Tu';
    }

    return this.userAliasById().get(userId) ?? 'Utilizator';
  }

  load(): void {
    this.loading.set(true);
    this.api.getAllPortfolios(this.includeDeleted).subscribe({
      next: (data) => this.portfolios.set(data),
      error: () => this.message.error('Nu am putut incarca portofoliile administrative.'),
      complete: () => this.loading.set(false)
    });
  }

  loadInsights(showSuccessMessage = false): void {
    this.insightsLoading.set(true);

    forkJoin({
      diversified: this.api
        .getMostDiversifiedPortfolios(8)
        .pipe(catchError(() => of([] as AdminDiversifiedPortfolioInsight[]))),
      deleted: this.api
        .getRecentlyDeletedPortfolios(30)
        .pipe(catchError(() => of([] as AdminDeletedPortfolioInsight[])))
    }).subscribe({
      next: ({ diversified, deleted }) => {
        this.diversifiedInsights.set(diversified);
        this.deletedInsights.set(deleted);

        if (showSuccessMessage) {
          this.message.success('Insight-urile administrative au fost actualizate.');
        }
      },
      complete: () => this.insightsLoading.set(false)
    });
  }

  createPortfolio(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const payload = this.createForm.getRawValue();
    this.api.createPortfolio(payload).subscribe({
      next: () => {
        this.message.success('Portofoliul a fost creat.');
        this.createForm.reset();
        this.load();
        this.loadInsights();
      },
      error: () => this.message.error('Crearea portofoliului a esuat.')
    });
  }

  deletePortfolio(portfolioId: number): void {
    this.api.deletePortfolio(portfolioId).subscribe({
      next: () => {
        this.message.success('Portofoliul a fost sters.');
        this.load();
        this.loadInsights();
      },
      error: () => this.message.error('Stergerea portofoliului a esuat.')
    });
  }

  refreshPortfolioPrices(portfolioId: number): void {
    this.api.refreshPortfolioPrices(portfolioId).subscribe({
      next: () => this.message.success('Cotatiile pentru acest portofoliu au fost puse in coada pentru actualizare.'),
      error: () => this.message.error('Nu am putut pune in coada cotatiile pentru portofoliu.')
    });
  }

  loadValuation(portfolioId: number): void {
    this.api.getPortfolioValuation(portfolioId).subscribe({
      next: (data) => {
        this.valuationData[portfolioId] = data;
        this.showValuation[portfolioId] = true;
      },
      error: () => this.message.error('Nu am putut incarca evaluarea portofoliului.')
    });
  }

  private loadCurrentUser(): void {
    this.api.whoAmI().subscribe({
      next: (user) => this.currentUser.set(user),
      error: () => this.currentUser.set(null)
    });
  }
}

