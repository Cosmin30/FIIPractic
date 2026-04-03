import { CommonModule } from '@angular/common';
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
import { ApiService } from '../../core/api.service';
import { Stock } from '../../core/models';
import { enterMotion, metricPulseMotion, staggerChildrenMotion } from '../../shared/ui-animations';

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
  styleUrl: './stocks-page.component.css',
  animations: [enterMotion, staggerChildrenMotion, metricPulseMotion]
})
export class StocksPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly message = inject(NzMessageService);

  readonly stocks = signal<Stock[]>([]);
  readonly loading = signal(false);
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
  }

  loadStocks(): void {
    this.loading.set(true);
    this.api.getStocks().subscribe({
      next: (data) => this.stocks.set(data),
      error: () => this.message.error('Nu am putut incarca simbolurile.'),
      complete: () => this.loading.set(false)
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
      },
      error: () => this.message.error('Crearea simbolului a esuat.')
    });
  }

  refreshPrice(symbol: string): void {
    this.api.refreshStock(symbol).subscribe({
      next: () => {
        this.message.success(`Cotatia pentru ${symbol} a fost actualizata.`);
        this.loadStocks();
      },
      error: () => this.message.error(`Nu am putut actualiza ${symbol}.`)
    });
  }

  refreshAllPrices(): void {
    this.api.refreshAllStocks().subscribe({
      next: () => {
        this.message.success('Toate cotatiile au fost puse in coada pentru actualizare.');
        this.loadStocks();
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
      },
      error: () => this.message.error('Actualizarea simbolului a esuat.')
    });
  }

  deleteStock(id: number): void {
    this.api.deleteStock(id).subscribe({
      next: () => {
        this.message.success(`Simbolul #${id} a fost sters.`);
        this.loadStocks();
      },
      error: () => this.message.error('Stergerea simbolului a esuat.')
    });
  }

  formatPrice(price: number | null): string {
    if (price === null) {
      return '-';
    }

    return `$${price.toFixed(2)}`;
  }
}
