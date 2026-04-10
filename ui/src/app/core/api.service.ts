import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminDeletedPortfolioInsight,
  AdminDiversifiedPortfolioInsight,
  BuyStockPayload,
  CreatePortfolioPayload,
  PortfolioBuyTimelinePoint,
  PortfolioExposureInsight,
  PortfolioOverviewInsight,
  PortfolioTopMoverInsight,
  Portfolio,
  SellHoldingsPayload,
  Stock,
  StockMostHeldInsight,
  StockStaleInsight,
  StockWatchlistCandidate,
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

  getStock(id: number): Observable<Stock> {
    return this.http.get<Stock>(`/api/stocks/${id}`);
  }

  getStaleStocks(minutes = 30): Observable<StockStaleInsight[]> {
    const params = new HttpParams().set('minutes', minutes);
    return this.http.get<StockStaleInsight[]>('/api/stocks/insights/stale', { params });
  }

  getMostHeldStocks(limit = 10): Observable<StockMostHeldInsight[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<StockMostHeldInsight[]>('/api/stocks/insights/most-held', { params });
  }

  getWatchlistCandidates(limit = 10): Observable<StockWatchlistCandidate[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<StockWatchlistCandidate[]>('/api/stocks/insights/watchlist', { params });
  }

  createStock(symbol: string): Observable<Stock> {
    const params = new HttpParams().set('symbol', symbol);
    return this.http.post<Stock>('/api/stocks', null, { params });
  }

  updateStock(id: number, symbol: string): Observable<Stock> {
    const params = new HttpParams().set('symbol', symbol);
    return this.http.put<Stock>(`/api/stocks/${id}`, null, { params });
  }

  refreshStock(symbol: string): Observable<{ status: string; message: string }> {
    return this.http.post<{ status: string; message: string }>(`/api/stocks/${symbol}/refresh`, {});
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

  sellHolding(portfolioId: number, holdingId: number): Observable<Portfolio> {
    return this.http.delete<Portfolio>(`/api/portfolios/${portfolioId}/holdings/${holdingId}`);
  }

  sellHoldings(portfolioId: number, payload: SellHoldingsPayload): Observable<Portfolio> {
    return this.http.delete<Portfolio>(`/api/portfolios/${portfolioId}/holdings`, { body: payload });
  }

  deletePortfolio(portfolioId: number): Observable<void> {
    return this.http.delete<void>(`/api/portfolios/${portfolioId}`);
  }

  getAllPortfolios(includeDeleted: boolean): Observable<Portfolio[]> {
    const params = new HttpParams().set('includeDeleted', includeDeleted);
    return this.http.get<Portfolio[]>('/api/portfolios/all', { params });
  }

  refreshAllStocks(): Observable<any> {
    return this.http.post('/api/stocks/refresh', {});
  }

  refreshPortfolioPrices(portfolioId: number): Observable<any> {
    return this.http.post(`/api/portfolios/${portfolioId}/refresh`, {});
  }

  getPortfolioValuation(portfolioId: number): Observable<any> {
    return this.http.get(`/api/portfolios/${portfolioId}/valuation`);
  }

  getPortfolioOverview(): Observable<PortfolioOverviewInsight> {
    return this.http.get<PortfolioOverviewInsight>('/api/portfolios/insights/overview');
  }

  getPortfolioExposure(): Observable<PortfolioExposureInsight[]> {
    return this.http.get<PortfolioExposureInsight[]>('/api/portfolios/insights/exposure');
  }

  getPortfolioTopMovers(limit = 5): Observable<PortfolioTopMoverInsight[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<PortfolioTopMoverInsight[]>('/api/portfolios/insights/top-movers', { params });
  }

  getPortfolioBuyTimeline(days = 30): Observable<PortfolioBuyTimelinePoint[]> {
    const params = new HttpParams().set('days', days);
    return this.http.get<PortfolioBuyTimelinePoint[]>('/api/portfolios/insights/buy-timeline', { params });
  }

  getMostDiversifiedPortfolios(limit = 10): Observable<AdminDiversifiedPortfolioInsight[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<AdminDiversifiedPortfolioInsight[]>('/api/portfolios/insights/diversified', { params });
  }

  getRecentlyDeletedPortfolios(days = 30): Observable<AdminDeletedPortfolioInsight[]> {
    const params = new HttpParams().set('days', days);
    return this.http.get<AdminDeletedPortfolioInsight[]>('/api/portfolios/insights/deleted', { params });
  }
}
