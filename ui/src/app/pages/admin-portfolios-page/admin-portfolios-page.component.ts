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
import { NzMessageService } from 'ng-zorro-antd/message';
import { ApiService } from '../../core/api.service';
import { Portfolio } from '../../core/models';
import { enterMotion, listItemMotion, metricPulseMotion, revealMotion, staggerChildrenMotion } from '../../shared/ui-animations';

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
    NzInputModule
  ],
  templateUrl: './admin-portfolios-page.component.html',
  styleUrl: './admin-portfolios-page.component.css',
  animations: [enterMotion, listItemMotion, staggerChildrenMotion, metricPulseMotion, revealMotion]
})
export class AdminPortfoliosPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly message = inject(NzMessageService);
  private readonly fb = inject(FormBuilder);

  readonly portfolios = signal<Portfolio[]>([]);
  readonly loading = signal(false);
  readonly totalPortfolios = computed(() => this.portfolios().length);
  readonly deletedCount = computed(() => this.portfolios().filter((portfolio) => portfolio.deleted).length);
  readonly activeCount = computed(() => this.totalPortfolios() - this.deletedCount());

  includeDeleted = false;

  createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    description: ['']
  });

  showValuation: Record<number, boolean> = {};
  valuationData: Record<number, any> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.getAllPortfolios(this.includeDeleted).subscribe({
      next: (data) => this.portfolios.set(data),
      error: () => this.message.error('Nu am putut incarca portofoliile administrative.'),
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
        this.load();
      },
      error: () => this.message.error('Crearea portofoliului a esuat.')
    });
  }

  deletePortfolio(portfolioId: number): void {
    this.api.deletePortfolio(portfolioId).subscribe({
      next: () => {
        this.message.success('Portofoliul a fost sters.');
        this.load();
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
}
