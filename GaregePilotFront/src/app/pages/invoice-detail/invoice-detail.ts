import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Invoice } from '../../core/models/invoice.model';
import { InvoiceApiService } from '../../core/services/invoice-api.service';

@Component({
  selector: 'app-invoice-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './invoice-detail.html',
  styleUrl: './invoice-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoiceDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(InvoiceApiService);

  protected readonly invoice = signal<Invoice | null>(null);
  protected readonly loading = signal(true);
  protected readonly downloading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Facture introuvable.');
      this.loading.set(false);
      return;
    }

    this.api
      .findById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (invoice) => this.invoice.set(invoice),
        error: () => this.error.set('Impossible de charger cette facture.'),
      });
  }

  protected downloadPdf(): void {
    const invoice = this.invoice();
    if (!invoice || this.downloading()) return;

    this.downloading.set(true);
    this.api
      .downloadPdf(invoice.id)
      .pipe(finalize(() => this.downloading.set(false)))
      .subscribe({
        next: (pdf) => {
          const url = URL.createObjectURL(pdf);
          const link = document.createElement('a');
          link.href = url;
          link.download = `facture-${invoice.invoiceNumber}.pdf`;
          link.click();
          URL.revokeObjectURL(url);
        },
        error: () => this.error.set('Impossible de télécharger le PDF.'),
      });
  }
}
