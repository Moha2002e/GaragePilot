import { Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounce, finalize, switchMap, tap, timer } from 'rxjs';
import { Client } from '../../core/models/client.model';
import { ClientApiService } from '../../core/services/client-api.service';

interface ReloadRequest {
  page: number;
  pageSize: number;
  debounce: boolean;
}

@Injectable()
export class ClientStateService {
  private readonly api = inject(ClientApiService);
  readonly clients = signal<Client[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly search = signal('');

  private readonly reload$ = new Subject<ReloadRequest>();

  constructor() {
    this.reload$
      .pipe(
        debounce((request) => timer(request.debounce ? 300 : 0)),
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap((request) =>
          this.api
            .findAll(request.page, request.pageSize, this.search())
            .pipe(finalize(() => this.loading.set(false))),
        ),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (response) => {
          this.clients.set(response.content);
          this.page.set(response.number);
          this.pageSize.set(response.size);
          this.totalElements.set(response.totalElements);
        },
        error: () => this.error.set('Impossible de charger les clients.'),
      });
  }

  load(page = this.page(), pageSize = this.pageSize()): void {
    this.reload$.next({ page, pageSize, debounce: false });
  }

  changePage(page: number): void {
    this.load(page, this.pageSize());
  }

  changePageSize(pageSize: number): void {
    this.load(0, pageSize);
  }

  setSearch(search: string): void {
    this.search.set(search);
    this.reload$.next({ page: 0, pageSize: this.pageSize(), debounce: true });
  }
}
