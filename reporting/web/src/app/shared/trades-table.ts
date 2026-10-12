import { Component, input } from '@angular/core';

import { count, dateTime, moneyExact } from '../core/format';
import { TradeRow } from '../core/models';

/** The latest trades for one client or one instrument. */
@Component({
  selector: 'app-trades-table',
  template: `
    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Time</th><th>Account</th><th>Ticker</th><th>Class</th><th>Side</th>
            <th class="num">Quantity</th><th class="num">Price</th><th class="num">Value</th><th>Status</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track row.trade_id) {
            <tr>
              <td>{{ dateTime(row.trade_time) }}</td>
              <td>{{ row.account_id }}</td>
              <td>{{ row.ticker }}</td>
              <td>{{ row.asset_class }}</td>
              <td>{{ row.trade_type }}</td>
              <td class="num">{{ count.format(row.quantity) }}</td>
              <td class="num">{{ row.price === null ? '–' : moneyExact.format(row.price) }}</td>
              <td class="num">{{ row.notional === null ? '–' : moneyExact.format(row.notional) }}</td>
              <td>{{ row.status }}</td>
            </tr>
          } @empty {
            <tr><td colspan="9" class="empty">No trades.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class TradesTableComponent {
  readonly rows = input.required<TradeRow[]>();

  protected readonly count = count;
  protected readonly moneyExact = moneyExact;
  protected readonly dateTime = dateTime;
}
