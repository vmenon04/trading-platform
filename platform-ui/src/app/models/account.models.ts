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

// POST /v1/accounts/{accountId}/trades (accountId is also required in the body)
export interface TradeRequest {
  accountId: string;
  instrumentId: number;
  side: TradeSide;
  quantity: number;
}

// 400 response body (ErrorResponseDTO)
export interface ErrorResponse {
  status: string;
  error: string;
  message: string;
  fieldErrors: { field: string; message: string }[];
}

// GET /v1/accounts/{accountId}/orders
export interface Trade {
  instrumentId: number;
  ticker: string;
  name: string;
  tradeId: string | null;
  side: TradeSide;
  quantity: number;
  ppu: number | null;
  totalPrice: number | null;
  status: TradeStatus;
  reason: string | null;
}
