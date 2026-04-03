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
import { ApiService } from '../../core/api.service';
import { Portfolio } from '../../core/models';
import { enterMotion, listItemMotion, metricPulseMotion, revealMotion, staggerChildrenMotion } from '../../shared/ui-animations';

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
  styleUrl: './portfolios-page.component.css',
  animations: [enterMotion, listItemMotion, staggerChildrenMotion, metricPulseMotion, revealMotion]
})
export class PortfoliosPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly message = inject(NzMessageService);

  readonly portfolios = signal<Portfolio[]>([]);
  readonly loading = signal(false);
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

  ngOnInit(): void {
    this.loadPortfolios();
  }

  loadPortfolios(): void {
    this.loading.set(true);
    this.api.getMyPortfolios().subscribe({
      next: (data) => {
        this.portfolios.set(data);
        data.forEach((portfolio) => this.ensureBuyForm(portfolio.id));
      },
      error: () => this.message.error('Nu am putut incarca portofoliile.'),
      complete: () => this.loading.set(false)
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
        this.loadPortfolios();
      },
      error: () => this.message.error('Crearea portofoliului a esuat.')
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
      quantity: Number(form.value['quantity']),
      purchasePrice: Number(form.value['purchasePrice'])
    };

    this.api.buyStock(portfolioId, payload).subscribe({
      next: () => {
        this.message.success(`Actiunea a fost cumparata in portofoliul #${portfolioId}.`);
        form.reset({ symbol: '', quantity: 1, purchasePrice: 1 });
        this.loadPortfolios();
      },
      error: () => this.message.error('Operatiunea de cumparare a esuat.')
    });
  }

  deletePortfolio(portfolioId: number): void {
    this.api.deletePortfolio(portfolioId).subscribe({
      next: () => {
        this.message.success(`Portofoliul #${portfolioId} a fost sters.`);
        this.loadPortfolios();
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

  showValuation: Record<number, boolean> = {};
  valuationData: Record<number, any> = {};

  loadValuation(portfolioId: number): void {
    this.api.getPortfolioValuation(portfolioId).subscribe({
      next: (data) => {
        this.valuationData[portfolioId] = data;
        this.showValuation[portfolioId] = true;
      },
      error: () => this.message.error('Nu am putut incarca evaluarea portofoliului.')
    });
  }

  private ensureBuyForm(portfolioId: number): void {
    if (this.buyForms[portfolioId]) {
      return;
    }

    this.buyForms[portfolioId] = this.fb.group({
      symbol: ['', [Validators.required, Validators.maxLength(10)]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      purchasePrice: [1, [Validators.required, Validators.min(0.01)]]
    });
  }

  formatMoney(value: number): string {
    return `$${value.toFixed(2)}`;
  }
}
