import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzMessageService } from 'ng-zorro-antd/message';
import { ApiService } from '../../core/api.service';
import { Portfolio } from '../../core/models';

@Component({
  selector: 'app-admin-portfolios-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NzCardModule,
    NzSwitchModule,
    NzTableModule,
    NzTagModule,
    NzButtonModule
  ],
  templateUrl: './admin-portfolios-page.component.html',
  styleUrl: './admin-portfolios-page.component.css'
})
export class AdminPortfoliosPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly message = inject(NzMessageService);

  readonly portfolios = signal<Portfolio[]>([]);
  readonly loading = signal(false);
  readonly totalPortfolios = computed(() => this.portfolios().length);
  readonly deletedCount = computed(() => this.portfolios().filter((portfolio) => portfolio.deleted).length);
  readonly activeCount = computed(() => this.totalPortfolios() - this.deletedCount());

  includeDeleted = false;

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
}
