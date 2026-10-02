import { CurrencyPipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardData, StatusSummary } from '../../core/models/dashboard.model';
import { DashboardApiService } from '../../core/services/dashboard-api.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements OnInit {
  private readonly api = inject(DashboardApiService);

  protected readonly data = signal<DashboardData | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly maxMonthlyAmount = computed(() => {
    const months = this.data()?.monthlyFinancials ?? [];
    return Math.max(1, ...months.flatMap((month) => [month.invoiced, month.collected]));
  });
  protected readonly invoiceTotal = computed(() =>
    (this.data()?.invoiceStatuses ?? []).reduce((sum, item) => sum + item.count, 0),
  );
  protected readonly pipelineMax = computed(() =>
    Math.max(1, ...(this.data()?.workshopPipeline ?? []).map((item) => item.count)),
  );

  ngOnInit(): void {
    this.api.get().subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les indicateurs du garage.');
        this.loading.set(false);
      },
    });
  }

  protected barHeight(amount: number): number {
    return Math.max(4, (amount / this.maxMonthlyAmount()) * 100);
  }

  protected pipelineWidth(count: number): number {
    return (count / this.pipelineMax()) * 100;
  }

  protected invoicePercent(item: StatusSummary): number {
    return this.invoiceTotal() === 0 ? 0 : (item.count / this.invoiceTotal()) * 100;
  }

  protected statusLabel(status: string): string {
    return (
      {
        DRAFT: 'Brouillons',
        ISSUED: 'Émises',
        PARTIALLY_PAID: 'Partiellement payées',
        PAID: 'Payées',
        OVERDUE: 'En retard',
        CANCELLED: 'Annulées',
        WAITING_FOR_APPROVAL: 'En attente d’accord',
        APPROVED: 'Approuvées',
        IN_PROGRESS: 'En cours',
        WAITING_FOR_PARTS: 'En attente de pièces',
        COMPLETED: 'Terminées',
        INVOICED: 'Facturées',
      }[status] ?? status
    );
  }
}
