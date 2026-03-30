import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BuyStockPayload,
  CreatePortfolioPayload,
  Portfolio,
  Stock,
  UserInfo
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  health(): Observable<string> {
    return this.http.get('/api/public/health', { responseType: 'text' });
  }

  whoAmI(): Observable<UserInfo> {
    return this.http.get<UserInfo>('/api/public/whoami');
  }

  getStocks(): Observable<Stock[]> {
    return this.http.get<Stock[]>('/api/stocks');
  }

  createStock(symbol: string): Observable<Stock> {
    const params = new HttpParams().set('symbol', symbol);
    return this.http.post<Stock>('/api/stocks', null, { params });
  }

  updateStock(id: number, symbol: string): Observable<Stock> {
    const params = new HttpParams().set('symbol', symbol);
    return this.http.put<Stock>(`/api/stocks/${id}`, null, { params });
  }

  refreshStock(symbol: string): Observable<Stock> {
    return this.http.post<Stock>(`/api/stocks/${symbol}/refresh`, {});
  }

  deleteStock(id: number): Observable<void> {
    return this.http.delete<void>(`/api/stocks/${id}`);
  }

  getMyPortfolios(): Observable<Portfolio[]> {
    return this.http.get<Portfolio[]>('/api/portfolios/my');
  }

  createPortfolio(payload: CreatePortfolioPayload): Observable<Portfolio> {
    return this.http.post<Portfolio>('/api/portfolios', payload);
  }

  buyStock(portfolioId: number, payload: BuyStockPayload): Observable<Portfolio> {
    return this.http.post<Portfolio>(`/api/portfolios/${portfolioId}/stocks`, payload);
  }

  deletePortfolio(portfolioId: number): Observable<void> {
    return this.http.delete<void>(`/api/portfolios/${portfolioId}`);
  }

  getAllPortfolios(includeDeleted: boolean): Observable<Portfolio[]> {
    const params = new HttpParams().set('includeDeleted', includeDeleted);
    return this.http.get<Portfolio[]>('/api/portfolios/all', { params });
  }
}
