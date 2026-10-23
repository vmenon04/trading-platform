import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, input, model, signal } from '@angular/core';

import { clientColor } from './charts';
import { ClientMatch } from './models';

let nextId = 0;

/**
 * A client the analyst picked: its id, the name to show on its chip and in the charts, and its colour slot.
 * The slot is kept for as long as the client stays picked, so removing one client never recolours the others.
 */
export interface PickedClient {
  id: number;
  name: string;
  slot: number;
}

/**
 * A type-ahead box that collects several clients. Each pick becomes a chip in that client's chart colour,
 * so the chip doubles as the key for the charts; the × on a chip drops it. At most `max` clients.
 */
@Component({
  selector: 'app-client-picker',
  template: `
    <label class="grow">Clients
      <input [attr.list]="listId" [placeholder]="full() ? 'Up to ' + max() + ' clients' : 'All clients — search name, email or ID'"
             [disabled]="full()" [value]="text()" (input)="typed($event)">
      <datalist [id]="listId">
        @for (option of options(); track option.client_id) {
          <option [value]="describe(option)"></option>
        }
      </datalist>
    </label>
    @if (picked().length) {
      <div class="picked" aria-label="Picked clients">
        @for (client of picked(); track client.id) {
          <span class="chip">
            <i [style.background]="color(client.slot)"></i>{{ client.name }} <small>#{{ client.id }}</small>
            <button type="button" (click)="remove(client)" [attr.aria-label]="'Remove ' + client.name">×</button>
          </span>
        }
      </div>
    }
  `,
  styles: `
    :host { display: contents; }
    .picked { display: flex; flex-wrap: wrap; gap: 6px; flex: 0 0 100%; }
    .chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 4px 4px 10px; border: 1px solid var(--line);
            border-radius: 999px; background: #f7faf8; font-size: 0.7rem; font-weight: 600; }
    .chip i { width: 8px; height: 8px; border-radius: 50%; }
    .chip button { min-height: 22px; width: 22px; padding: 0; border: 0; border-radius: 50%; background: transparent; font-size: 0.85rem; line-height: 1; }
    .chip button:hover { background: var(--line) !important; }
  `,
})
export class ClientPicker {
  readonly picked = model<PickedClient[]>([]);
  readonly max = input(8);

  protected readonly listId = `client-picker-${nextId++}`;
  protected readonly text = signal('');
  protected readonly options = signal<ClientMatch[]>([]);

  private readonly http = inject(HttpClient);
  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  protected full(): boolean {
    return this.picked().length >= this.max();
  }

  // options end in "(#123)", which is how a pick is told apart from typing
  protected describe(client: ClientMatch): string {
    return `${client.name} · ${client.email} (#${client.client_id})`;
  }

  protected color(slot: number): string {
    return clientColor(slot);
  }

  protected remove(client: PickedClient): void {
    this.picked.update((picked) => picked.filter((one) => one.id !== client.id));
  }

  protected typed(event: Event): void {
    clearTimeout(this.timer);
    const box = event.target as HTMLInputElement;
    this.text.set(box.value);

    const id = Number(box.value.match(/\(#(\d+)\)$/)?.[1]);
    if (id) {
      const match = this.options().find((option) => option.client_id === id);
      if (!this.picked().some((one) => one.id === id)) {
        this.picked.update((picked) => [...picked, { id, name: match?.name ?? `Client #${id}`, slot: freeSlot(picked) }]);
      }
      this.text.set('');
      box.value = '';
      return;
    }
    const search = box.value.trim();
    if (!search) {
      this.options.set([]);
      return;
    }
    this.timer = setTimeout(() => {
      this.http
        .get<ClientMatch[]>('/api/clients', { params: { q: search, limit: 20 } })
        .subscribe((matches) => this.options.set(matches));
    }, 250);
  }
}

/** The lowest colour slot no picked client is using. */
function freeSlot(picked: PickedClient[]): number {
  let slot = 0;
  while (picked.some((one) => one.slot === slot)) slot++;
  return slot;
}
