import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Pagination } from '../../core/components/pagination/pagination';
import { InvoiceStatus } from '../../core/models/invoice.model';
import { InvoiceStateService } from './invoice-state.service';

@Component({
  selector: 'app-invoices',
  imports: [CurrencyPipe, DatePipe, RouterLink, Pagination],
  providers: [InvoiceStateService],
  templateUrl: './invoices.html',
  styleUrl: './invoices.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Invoices implements OnInit {
  protected readonly state = inject(InvoiceStateService);

  ngOnInit(): void {
    this.state.load();
  }

  protected statusLabel(status: InvoiceStatus): string {
    return {
      DRAFT: 'Brouillon',
      ISSUED: 'Émise',
      PARTIALLY_PAID: 'Partielle',
      PAID: 'Payée',
      OVERDUE: 'En retard',
      CANCELLED: 'Annulée',
    }[status];
  }
}
