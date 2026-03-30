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
