import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCollapseModule } from 'ng-zorro-antd/collapse';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzTableModule } from 'ng-zorro-antd/table';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import {
  Holding,
  Portfolio,
  PortfolioBuyTimelinePoint,
  PortfolioExposureInsight,
  PortfolioOverviewInsight,
  PortfolioTopMoverInsight
} from '../../core/models';

@Component({
  selector: 'app-portfolios-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzCardModule,
    NzButtonModule,
    NzCollapseModule,
    NzFormModule,
    NzInputModule,
    NzInputNumberModule,
    NzTableModule,
    NzPopconfirmModule
  ],
  templateUrl: './portfolios-page.component.html',
  styleUrl: './portfolios-page.component.css'
})
export class PortfoliosPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly message = inject(NzMessageService);
  private insightsFallbackNoticeShown = false;
  private readonly quantityPrecision = 4;
  private readonly quantityEpsilon = 0.0001;

  readonly portfolios = signal<Portfolio[]>([]);
  readonly loading = signal(false);
  readonly insightsLoading = signal(false);
  readonly overview = signal<PortfolioOverviewInsight | null>(null);
  readonly exposure = signal<PortfolioExposureInsight[]>([]);
  readonly topMovers = signal<PortfolioTopMoverInsight[]>([]);
  readonly buyTimeline = signal<PortfolioBuyTimelinePoint[]>([]);
  readonly totalPortfolios = computed(() => this.portfolios().length);
  readonly totalHoldings = computed(() =>
    this.portfolios().reduce((total, portfolio) => total + portfolio.holdings.length, 0)
  );
  readonly investedCapital = computed(() =>
    this.portfolios().reduce(
      (total, portfolio) =>
        total + portfolio.holdings.reduce((sum, holding) => sum + holding.quantity * holding.purchasePrice, 0),
      0
    )
  );

  readonly createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    description: ['']
  });

  readonly buyForms: Record<number, FormGroup> = {};
  readonly sellingHoldingKeys = signal<Set<string>>(new Set());

  ngOnInit(): void {
    this.loadPortfolios();
    this.loadInsights();
  }

  loadPortfolios(): void {
    this.loading.set(true);
    this.api.getMyPortfolios().subscribe({
      next: (data) => {
        this.portfolios.set(data);
        this.overview.set(this.buildOverviewFromPortfolios(data));
        data.forEach((portfolio) => this.ensureBuyForm(portfolio.id));
      },
      error: () => this.message.error('Nu am putut incarca lista de portofolii.'),
      complete: () => this.loading.set(false)
    });
  }

  loadInsights(showSuccessMessage = false): void {
    this.insightsLoading.set(true);
    let usedFallbackData = false;

    forkJoin({
      overview: this.api
        .getPortfolioOverview()
        .pipe(
          catchError(() => {
            usedFallbackData = true;
            return of(this.overview() ?? this.buildOverviewFromPortfolios(this.portfolios()));
          })
        ),
      exposure: this.api
        .getPortfolioExposure()
        .pipe(
          catchError(() => {
            usedFallbackData = true;
            return of(this.buildExposureFromPortfolios(this.portfolios()));
          })
        ),
      topMovers: this.api
        .getPortfolioTopMovers(8)
        .pipe(
          catchError(() => {
            usedFallbackData = true;
            return of([] as PortfolioTopMoverInsight[]);
          })
        ),
      buyTimeline: this.api
        .getPortfolioBuyTimeline(30)
        .pipe(
          catchError(() => {
            usedFallbackData = true;
            return of(this.buildBuyTimelineFromPortfolios(this.portfolios()));
          })
        )
    }).subscribe({
      next: ({ overview, exposure, topMovers, buyTimeline }) => {
        this.overview.set(overview);
        this.exposure.set(exposure);
        this.topMovers.set(topMovers);
        this.buyTimeline.set(buyTimeline);

        if (usedFallbackData && !this.insightsFallbackNoticeShown) {
          this.message.warning('Unele analize nu s-au incarcat din backend. Afisam temporar date locale.');
          this.insightsFallbackNoticeShown = true;
        }

        if (!usedFallbackData) {
          this.insightsFallbackNoticeShown = false;
        }

        if (showSuccessMessage) {
          this.message.success('Analizele portofoliilor au fost actualizate.');
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
        this.message.success('Portofoliul a fost creat cu succes.');
        this.createForm.reset();
        this.loadPortfolios();
        this.loadInsights();
      },
      error: () => this.message.error('Nu am putut crea portofoliul.')
    });
  }

  buyStock(portfolioId: number): void {
    const form = this.buyForms[portfolioId];
    if (!form || form.invalid) {
      form?.markAllAsTouched();
      return;
    }

    const payload = {
      symbol: String(form.value['symbol']).trim().toUpperCase(),
      quantity: Number(form.value['quantity'])
    };

    this.api.buyStock(portfolioId, payload).subscribe({
      next: () => {
        this.message.success(`Actiunea a fost cumparata in portofoliul #${portfolioId}.`);
        form.reset({ symbol: '', quantity: 1 });
        this.loadPortfolios();
        this.loadInsights();
      },
      error: () => this.message.error('Nu am putut finaliza cumpararea.')
    });
  }

  deletePortfolio(portfolioId: number): void {
    this.api.deletePortfolio(portfolioId).subscribe({
      next: () => {
        this.message.success(`Portofoliul #${portfolioId} a fost sters cu succes.`);
        this.loadPortfolios();
        this.loadInsights();
      },
      error: () => this.message.error('Nu am putut sterge portofoliul.')
    });
  }

  refreshPortfolioPrices(portfolioId: number): void {
    this.api.refreshPortfolioPrices(portfolioId).subscribe({
      next: () => this.message.success('Actualizarea preturilor pentru acest portofoliu a fost pornita.'),
      error: () => this.message.error('Nu am putut porni actualizarea preturilor pentru portofoliu.')
    });
  }

  sellAllHoldings(portfolio: Portfolio): void {
    const holdingIds = portfolio.holdings.map((holding) => holding.id);
    if (holdingIds.length === 0) {
      this.message.info('Portofoliul nu are pozitii deschise.');
      return;
    }

    this.api.sellHoldings(portfolio.id, { holdingIds }).subscribe({
      next: () => {
        this.message.success(`Toate pozitiile din portofoliul #${portfolio.id} au fost vandute cu succes.`);
        this.clearValuationCache(portfolio.id);
        this.loadPortfolios();
        this.loadInsights();
      },
      error: () => this.message.error('Nu am putut vinde toate pozitiile.')
    });
  }

  sellHoldingWithQuantity(portfolioId: number, holding: Holding): void {
    if (this.isSellingHolding(portfolioId, holding.id)) {
      return;
    }

    const sellQuantity = this.promptSellQuantity(holding.quantity);
    if (sellQuantity === null) {
      return;
    }

    if (this.quantitiesEqual(sellQuantity, holding.quantity)) {
      this.sellHoldingCompletely(portfolioId, holding);
      return;
    }

    this.sellHoldingPartially(portfolioId, holding, sellQuantity);
  }

  isSellingHolding(portfolioId: number, holdingId: number): boolean {
    return this.sellingHoldingKeys().has(this.getHoldingKey(portfolioId, holdingId));
  }

  showValuation: Record<number, boolean> = {};
  valuationData: Record<number, any> = {};

  loadValuation(portfolioId: number): void {
    this.api.getPortfolioValuation(portfolioId).subscribe({
      next: (data) => {
        this.valuationData[portfolioId] = data;
        this.showValuation[portfolioId] = true;
      },
      error: () => this.message.error('Nu am putut incarca evaluarea acestui portofoliu.')
    });
  }

  private ensureBuyForm(portfolioId: number): void {
    if (this.buyForms[portfolioId]) {
      return;
    }

    this.buyForms[portfolioId] = this.fb.group({
      symbol: ['', [Validators.required, Validators.maxLength(10)]],
      quantity: [1, [Validators.required, Validators.min(this.quantityEpsilon)]]
    });
  }

  private sellHoldingCompletely(portfolioId: number, holding: Holding): void {
    this.setHoldingSelling(portfolioId, holding.id, true);

    this.api
      .sellHolding(portfolioId, holding.id)
      .pipe(finalize(() => this.setHoldingSelling(portfolioId, holding.id, false)))
      .subscribe({
        next: () => {
          this.message.success(
            `Ai vandut toate cele ${this.formatQuantity(holding.quantity)} actiuni ${holding.symbol}.`
          );
          this.clearValuationCache(portfolioId);
          this.loadPortfolios();
          this.loadInsights();
        },
        error: () => this.message.error('Nu am putut vinde aceasta pozitie.')
      });
  }

  private sellHoldingPartially(portfolioId: number, holding: Holding, sellQuantity: number): void {
    const remainingQuantity = this.roundQuantity(holding.quantity - sellQuantity);

    this.setHoldingSelling(portfolioId, holding.id, true);

    this.api
      .sellHoldingQuantity(portfolioId, holding.id, sellQuantity)
      .pipe(finalize(() => this.setHoldingSelling(portfolioId, holding.id, false)))
      .subscribe({
        next: () => {
          this.message.success(
            `Ai vandut ${this.formatQuantity(sellQuantity)} actiuni ${holding.symbol}. Au ramas ${this.formatQuantity(remainingQuantity)} actiuni in portofoliu.`
          );
          this.clearValuationCache(portfolioId);
          this.loadPortfolios();
          this.loadInsights();
        },
        error: () => {
          this.message.error('Nu am putut finaliza vanzarea partiala. Reincarca portofoliul si incearca din nou.');
          this.loadPortfolios();
          this.loadInsights();
        }
      });
  }

  private promptSellQuantity(maxQuantity: number): number | null {
    const input = window.prompt(
      `Introdu cantitatea pe care vrei sa o vinzi (intre 0.0001 si ${this.formatQuantity(maxQuantity)}).`,
      this.formatQuantity(maxQuantity)
    );

    if (input === null) {
      return null;
    }

    const normalized = input.trim().replace(',', '.');
    if (!/^\d+(\.\d{1,4})?$/.test(normalized)) {
      this.message.error('Cantitatea trebuie sa fie un numar pozitiv, cu cel mult 4 zecimale.');
      return null;
    }

    const quantity = this.roundQuantity(Number(normalized));
    if (!Number.isFinite(quantity) || quantity < this.quantityEpsilon || quantity > maxQuantity + this.quantityEpsilon) {
      this.message.error(`Cantitatea trebuie sa fie intre 0.0001 si ${this.formatQuantity(maxQuantity)}.`);
      return null;
    }

    return quantity;
  }

  private clearValuationCache(portfolioId: number): void {
    this.showValuation[portfolioId] = false;
    delete this.valuationData[portfolioId];
  }

  private setHoldingSelling(portfolioId: number, holdingId: number, selling: boolean): void {
    const key = this.getHoldingKey(portfolioId, holdingId);
    this.sellingHoldingKeys.update((current) => {
      const next = new Set(current);
      if (selling) {
        next.add(key);
      } else {
        next.delete(key);
      }
      return next;
    });
  }

  private getHoldingKey(portfolioId: number, holdingId: number): string {
    return `${portfolioId}:${holdingId}`;
  }

  private roundQuantity(value: number): number {
    const factor = 10 ** this.quantityPrecision;
    return Math.round(value * factor) / factor;
  }

  private quantitiesEqual(left: number, right: number): boolean {
    return Math.abs(left - right) < this.quantityEpsilon;
  }

  private buildOverviewFromPortfolios(portfolios: Portfolio[]): PortfolioOverviewInsight {
    const holdingCount = portfolios.reduce((total, portfolio) => total + portfolio.holdings.length, 0);
    const invested = portfolios.reduce(
      (portfolioTotal, portfolio) =>
        portfolioTotal
        + portfolio.holdings.reduce(
          (holdingTotal, holding) => holdingTotal + holding.quantity * holding.purchasePrice,
          0
        ),
      0
    );

    return {
      userId: '',
      portfolioCount: portfolios.length,
      holdingCount,
      invested,
      currentValue: invested
    };
  }

  private buildExposureFromPortfolios(portfolios: Portfolio[]): PortfolioExposureInsight[] {
    const bySymbol = new Map<string, { totalQuantity: number; invested: number }>();

    portfolios.forEach((portfolio) => {
      portfolio.holdings.forEach((holding) => {
        const current = bySymbol.get(holding.symbol) ?? { totalQuantity: 0, invested: 0 };
        current.totalQuantity += holding.quantity;
        current.invested += holding.quantity * holding.purchasePrice;
        bySymbol.set(holding.symbol, current);
      });
    });

    return Array.from(bySymbol.entries())
      .map(([symbol, values]) => ({
        symbol,
        totalQuantity: values.totalQuantity,
        invested: values.invested,
        currentValue: values.invested,
        profitLoss: 0,
        profitLossPercent: 0
      }))
      .sort((left, right) => right.invested - left.invested);
  }

  private buildBuyTimelineFromPortfolios(portfolios: Portfolio[]): PortfolioBuyTimelinePoint[] {
    const timelineByDay = new Map<string, { trades: number; totalQuantity: number; invested: number }>();

    portfolios.forEach((portfolio) => {
      portfolio.holdings.forEach((holding) => {
        const day = holding.purchasedAt ? holding.purchasedAt.slice(0, 10) : 'n/a';
        const current = timelineByDay.get(day) ?? { trades: 0, totalQuantity: 0, invested: 0 };

        current.trades += 1;
        current.totalQuantity += holding.quantity;
        current.invested += holding.quantity * holding.purchasePrice;

        timelineByDay.set(day, current);
      });
    });

    return Array.from(timelineByDay.entries())
      .map(([day, values]) => ({
        day,
        trades: values.trades,
        totalQuantity: values.totalQuantity,
        invested: values.invested
      }))
      .sort((left, right) => right.day.localeCompare(left.day))
      .slice(0, 30);
  }

  formatMoney(value: number): string {
    return `$${value.toFixed(2)}`;
  }

  formatQuantity(value: number): string {
    return new Intl.NumberFormat('ro-RO', {
      minimumFractionDigits: 0,
      maximumFractionDigits: this.quantityPrecision
    }).format(value);
  }
}

