import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { catchError, forkJoin, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Stock, StockMostHeldInsight, StockStaleInsight, StockWatchlistCandidate } from '../../core/models';

@Component({
  selector: 'app-stocks-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzCardModule,
    NzButtonModule,
    NzFormModule,
    NzInputModule,
    NzTableModule,
    NzTagModule,
    NzPopconfirmModule
  ],
  templateUrl: './stocks-page.component.html',
  styleUrl: './stocks-page.component.css'
})
export class StocksPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly message = inject(NzMessageService);

  readonly stocks = signal<Stock[]>([]);
  readonly loading = signal(false);
  readonly insightsLoading = signal(false);
  readonly deletingStockIds = signal<Set<number>>(new Set());
  readonly staleStocks = signal<StockStaleInsight[]>([]);
  readonly mostHeldStocks = signal<StockMostHeldInsight[]>([]);
  readonly watchlistCandidates = signal<StockWatchlistCandidate[]>([]);
  readonly totalStocks = computed(() => this.stocks().length);
  readonly pricedStocks = computed(() => this.stocks().filter((stock) => stock.currentPrice !== null).length);
  readonly averagePrice = computed(() => {
    const values = this.stocks()
      .map((stock) => stock.currentPrice)
      .filter((price): price is number => price !== null);

    if (!values.length) {
      return null;
    }

    return values.reduce((sum, price) => sum + price, 0) / values.length;
  });

  readonly createForm = this.fb.nonNullable.group({
    symbol: ['', [Validators.required, Validators.maxLength(10)]]
  });

  ngOnInit(): void {
    this.loadStocks();
    this.loadInsights();
  }

  loadStocks(): void {
    this.loading.set(true);
    this.api.getStocks().subscribe({
      next: (data) => this.stocks.set(data),
      error: () => this.message.error('Nu am putut incarca simbolurile.'),
      complete: () => this.loading.set(false)
    });
  }

  loadInsights(showSuccessMessage = false): void {
    this.insightsLoading.set(true);

    forkJoin({
      stale: this.api.getStaleStocks(60).pipe(catchError(() => of([] as StockStaleInsight[]))),
      mostHeld: this.api.getMostHeldStocks(8).pipe(catchError(() => of([] as StockMostHeldInsight[]))),
      watchlist: this.api.getWatchlistCandidates(8).pipe(catchError(() => of([] as StockWatchlistCandidate[])))
    }).subscribe({
      next: ({ stale, mostHeld, watchlist }) => {
        this.staleStocks.set(stale);
        this.mostHeldStocks.set(mostHeld);
        this.watchlistCandidates.set(watchlist);

        if (showSuccessMessage) {
          this.message.success('Insight-urile de piata au fost actualizate.');
        }
      },
      complete: () => this.insightsLoading.set(false)
    });
  }

  createStock(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    const symbol = this.createForm.getRawValue().symbol.trim().toUpperCase();
    this.api.createStock(symbol).subscribe({
      next: () => {
        this.message.success(`Simbolul ${symbol} a fost creat.`);
        this.createForm.reset();
        this.loadStocks();
        this.loadInsights();
      },
      error: () => this.message.error('Crearea simbolului a esuat.')
    });
  }

  refreshPrice(symbol: string): void {
    this.api.refreshStock(symbol).subscribe({
      next: () => {
        this.message.success(`Cotatia pentru ${symbol} a fost actualizata.`);
        this.loadStocks();
        this.loadInsights();
      },
      error: () => this.message.error(`Nu am putut actualiza ${symbol}.`)
    });
  }

  refreshAllPrices(): void {
    this.api.refreshAllStocks().subscribe({
      next: () => {
        this.message.success('Toate cotatiile au fost puse in coada pentru actualizare.');
        this.loadStocks();
        this.loadInsights();
      },
      error: () => this.message.error('Nu am putut pune in coada toate cotatiile.')
    });
  }

  updateStock(stock: Stock): void {
    const updated = window.prompt('Noul simbol', stock.symbol);
    if (!updated) {
      return;
    }

    this.api.updateStock(stock.id, updated.trim().toUpperCase()).subscribe({
      next: () => {
        this.message.success(`Simbolul #${stock.id} a fost actualizat.`);
        this.loadStocks();
        this.loadInsights();
      },
      error: () => this.message.error('Actualizarea simbolului a esuat.')
    });
  }

  deleteStock(id: number): void {
    if (this.isDeleting(id)) {
      return;
    }

    this.setDeleting(id, true);
    this.api.deleteStock(id).subscribe({
      next: () => {
        this.message.success(`Simbolul #${id} a fost sters.`);
        this.loadStocks();
        this.loadInsights();
      },
      error: (error: unknown) => this.message.error(this.resolveDeleteError(error)),
      complete: () => this.setDeleting(id, false)
    });
  }

  isDeleting(id: number): boolean {
    return this.deletingStockIds().has(id);
  }

  private setDeleting(id: number, deleting: boolean): void {
    this.deletingStockIds.update((current) => {
      const next = new Set(current);

      if (deleting) {
        next.add(id);
      } else {
        next.delete(id);
      }

      return next;
    });
  }

  private resolveDeleteError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const serverMessage = this.extractErrorMessage(error);

      if (error.status === 409) {
        return serverMessage ?? 'Simbolul este folosit in portofolii si nu poate fi sters. Sterge intai pozitiile care il folosesc.';
      }

      if (error.status === 404) {
        return 'Simbolul nu mai exista.';
      }

      return serverMessage ?? 'Stergerea simbolului a esuat.';
    }

    return 'Stergerea simbolului a esuat.';
  }

  private extractErrorMessage(error: HttpErrorResponse): string | null {
    const payload = error.error;

    if (typeof payload === 'string') {
      const trimmed = payload.trim();
      return trimmed.length > 0 ? trimmed : null;
    }

    if (payload && typeof payload === 'object') {
      const message = (payload as { message?: unknown }).message;
      if (typeof message === 'string') {
        const trimmed = message.trim();
        return trimmed.length > 0 ? trimmed : null;
      }
    }

    return null;
  }

  formatPrice(price: number | null): string {
    if (price === null) {
      return '-';
    }

    return `$${price.toFixed(2)}`;
  }
}

