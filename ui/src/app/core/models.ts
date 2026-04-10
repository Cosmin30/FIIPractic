export interface UserInfo {
  userId: string | null;
  username: string | null;
  email: string | null;
  roles: string[];
}

export interface Stock {
  id: number;
  symbol: string;
  currentPrice: number | null;
  lastPriceUpdate: string | null;
  inUse?: boolean;
}

export interface Holding {
  id: number;
  symbol: string;
  quantity: number;
  purchasePrice: number;
  purchasedAt: string;
}

export interface Portfolio {
  id: number;
  name: string;
  description: string | null;
  holdings: Holding[];
  createdAt: string;
  deleted?: boolean;
  deletedBy?: string | null;
  deletedAt?: string | null;
}

export interface CreatePortfolioPayload {
  name: string;
  description?: string;
}

export interface BuyStockPayload {
  symbol: string;
  quantity: number;
  purchasePrice: number;
}

export interface SellHoldingsPayload {
  holdingIds: number[];
}

export interface StockStaleInsight {
  stockId: number;
  symbol: string;
  currentPrice: number;
  lastPriceUpdate: string | null;
}

export interface StockMostHeldInsight {
  symbol: string;
  holdingCount: number;
  portfolioCount: number;
  totalQuantity: number;
  currentPrice: number;
}

export interface StockWatchlistCandidate {
  symbol: string;
  buys: number;
  totalQuantity: number;
  lastBuyAt: string | null;
  currentPrice: number;
}

export interface PortfolioOverviewInsight {
  userId: string;
  portfolioCount: number;
  holdingCount: number;
  invested: number;
  currentValue: number;
}

export interface PortfolioExposureInsight {
  symbol: string;
  totalQuantity: number;
  invested: number;
  currentValue: number;
  profitLoss: number;
  profitLossPercent: number;
}

export interface PortfolioTopMoverInsight {
  symbol: string;
  totalQuantity: number;
  averageBuyPrice: number;
  currentPrice: number;
  movePercent: number;
}

export interface PortfolioBuyTimelinePoint {
  day: string;
  trades: number;
  totalQuantity: number;
  invested: number;
}

export interface AdminDiversifiedPortfolioInsight {
  portfolioId: number;
  portfolioName: string;
  userId: string;
  positions: number;
  uniqueSymbols: number;
  marketValue: number;
}

export interface AdminDeletedPortfolioInsight {
  portfolioId: number;
  portfolioName: string;
  userId: string;
  deletedBy: string;
  deletedAt: string | null;
}
