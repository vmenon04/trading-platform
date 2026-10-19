// Shapes of the JSON returned by the Account & Trade Management Service
// (docs/api/account-trade-management-service.yaml). UUID -> string, BigDecimal/Long -> number.

export type TradeSide = 'BUY' | 'SELL';
export type TradeStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'FULFILLED';

// GET /v1/accounts/{accountId}
export interface AccountSummary {
  accountId: string;
  investmentValue: number;
  cashBalance: number;
  costBasis: number;
}

// GET /v1/accounts/{accountId}/holdings
export interface AccountHolding {
  accountId: string;
  name: string;
  ticker: string;
  quantity: number;
  price: number;
}

// GET /v1/accounts/{accountId}/orders
export interface Trade {
  instrumentId: number;
  ticker: string;
  tradeId: string | null;
  side: TradeSide;
  quantity: number;
  ppu: number | null;
  totalPrice: number | null;
  status: TradeStatus;
  reason: string | null;
}
