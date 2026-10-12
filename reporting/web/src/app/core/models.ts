// The shapes the reporting API (reporting/api.py) sends back. Dates arrive as ISO strings.

export type Grain = 'day' | 'week' | 'month' | 'quarter' | 'year';
export type OrderStatus = 'PENDING' | 'ACCEPTED' | 'FULFILLED' | 'REJECTED';
export type Side = 'BUY' | 'SELL';

export interface SyncStatus {
  last_sync: { finished_at: string; watermark: string | null; trades_copied: number } | null;
  stale: boolean;
  default_start: string;
  default_end: string; // exclusive
  max_rows: number;
}

export interface VolumeRow {
  period: string;
  trade_count: number;
  notional: number;
  units: number;
  active_accounts: number;
}

export interface BuySellRow {
  period: string;
  BUY: number;
  SELL: number;
  net: number;
  buy_trades: number;
  sell_trades: number;
}

export interface StatusMixRow extends Record<OrderStatus, number> {
  period: string;
  orders: number;
  fill_rate: number | null;
  reject_rate: number | null;
}

export interface PriceRow {
  period: string;
  avg_price: number | null;
  low_price: number | null;
  high_price: number | null;
}

export interface AssetClassRow {
  asset_class: string;
  trade_count: number;
  notional: number;
  pct_of_notional: number;
}

export interface TopClientRow {
  client_id: number;
  client_name: string;
  trade_count: number;
  notional: number;
}

export interface TopInstrumentRow {
  instrument_id: number;
  ticker: string;
  name: string;
  asset_class: string;
  trade_count: number;
  notional: number;
}

export interface TradeRow {
  trade_id: number;
  trade_time: string;
  account_id: number;
  ticker: string;
  asset_class: string;
  trade_type: Side;
  quantity: number;
  price: number | null;
  notional: number | null;
  status: OrderStatus;
}

export interface OrderRow extends TradeRow {
  status_time: string;
  clients: string | null;
  instrument: string;
}

export interface OrdersPage {
  total: number;
  page: number;
  page_size: number;
  pages: number;
  rows: OrderRow[];
}

export interface StatusStep {
  status: OrderStatus;
  status_time: string;
}

export interface ClientMatch {
  client_id: number;
  name: string;
  email: string;
  username: string | null;
}

export interface InstrumentMatch {
  instrument_id: number;
  ticker: string;
  name: string;
  asset_class: string;
}
