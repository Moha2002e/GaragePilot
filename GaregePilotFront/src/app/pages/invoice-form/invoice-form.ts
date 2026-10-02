import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, forkJoin, switchMap } from 'rxjs';
import { Client } from '../../core/models/client.model';
import { Invoice, InvoiceItemRequest, InvoiceRequest } from '../../core/models/invoice.model';
import { Vehicle } from '../../core/models/vehicle.model';
import { ClientApiService } from '../../core/services/client-api.service';
import { InvoiceApiService } from '../../core/services/invoice-api.service';
import { VehicleApiService } from '../../core/services/vehicle-api.service';

@Component({
  selector: 'app-invoice-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './invoice-form.html',
  styleUrl: './invoice-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoiceForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly clientsApi = inject(ClientApiService);
  private readonly vehiclesApi = inject(VehicleApiService);
  private readonly invoicesApi = inject(InvoiceApiService);

  protected readonly clients = signal<Client[]>([]);
  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly currentInvoice = signal<Invoice | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly invoiceId = this.route.snapshot.paramMap.get('id');
  protected readonly editing = this.invoiceId !== null;
  private readonly clientSearch$ = new Subject<string>();
  private readonly vehicleSearch$ = new Subject<string>();

  protected readonly form = this.fb.nonNullable.group({
    clientId: ['', Validators.required],
    vehicleId: ['', Validators.required],
    issueDate: [this.today(), Validators.required],
    dueDate: [''],
    items: this.fb.array([]),
  });

  protected availableVehicles(): Vehicle[] {
    const clientId = this.form.controls.clientId.value;
    return this.vehicles().filter((vehicle) => vehicle.clientId === clientId);
  }

  get items(): FormArray {
    return this.form.controls.items;
  }

  ngOnInit(): void {
    const invoiceRequest = this.invoiceId ? this.invoicesApi.findById(this.invoiceId) : undefined;

    forkJoin({
      clients: this.clientsApi.findAll(0, 20),
      ...(invoiceRequest ? { invoice: invoiceRequest } : {}),
    }).subscribe({
      next: (result) => {
        this.clients.set(result.clients.content);
        const invoice = 'invoice' in result ? (result.invoice as Invoice) : null;
        if (invoice) {
          this.ensureClient(invoice.clientId);
          this.populate(invoice);
        } else {
          this.addItem();
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les données de la facture.');
        this.loading.set(false);
      },
    });

    this.form.controls.clientId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((clientId) => {
        this.form.controls.vehicleId.setValue('', { emitEvent: false });
        this.vehicles.set([]);
        if (clientId) {
          this.loadVehicles(clientId);
        }
      });

    this.clientSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((search) => this.clientsApi.findAll(0, 20, search)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (page) => this.clients.set(page.content),
        error: () => this.error.set('Impossible de rechercher les clients.'),
      });

    this.vehicleSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((search) =>
          this.vehiclesApi.findAll(0, 20, search, this.form.controls.clientId.value),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (page) => this.vehicles.set(page.content),
        error: () => this.error.set('Impossible de rechercher les véhicules.'),
      });
  }

  protected searchClients(search: string): void {
    this.clientSearch$.next(search);
  }

  protected searchVehicles(search: string): void {
    if (!this.form.controls.clientId.value) return;
    this.vehicleSearch$.next(search);
  }

  protected addItem(item?: Partial<InvoiceItemRequest>): void {
    this.items.push(
      this.fb.nonNullable.group({
        type: [item?.type ?? 'SERVICE', Validators.required],
        description: [item?.description ?? '', Validators.required],
        quantity: [item?.quantity ?? 1, [Validators.required, Validators.min(0.01)]],
        unitPrice: [item?.unitPrice ?? 0, [Validators.required, Validators.min(0)]],
        vatRate: [item?.vatRate ?? 21, [Validators.required, Validators.min(0)]],
      }),
    );
  }

  protected removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  protected clientLabel(client: Client): string {
    return client.companyName || [client.firstName, client.lastName].filter(Boolean).join(' ');
  }

  protected vehicleLabel(vehicle: Vehicle): string {
    return `${vehicle.licensePlate} — ${vehicle.brand} ${vehicle.model}`;
  }

  protected submit(): void {
    if (this.form.invalid || this.items.length === 0) {
      this.form.markAllAsTouched();
      this.error.set('Complète les champs obligatoires et ajoute au moins une ligne.');
      return;
    }

    const value = this.form.getRawValue();
    const current = this.currentInvoice();
    const request: InvoiceRequest = {
      clientId: value.clientId,
      vehicleId: value.vehicleId,
      workOrderId: current?.workOrderId,
      quoteId: current?.quoteId,
      issueDate: value.issueDate,
      dueDate: value.dueDate || undefined,
      items: value.items as InvoiceItemRequest[],
    };
    const operation =
      this.invoiceId === null
        ? this.invoicesApi.create(request)
        : this.invoicesApi.update(this.invoiceId, request);

    this.saving.set(true);
    this.error.set(null);
    operation.subscribe({
      next: (invoice) => this.router.navigate(['/invoices', invoice.id]),
      error: (response) => {
        this.error.set(response.error?.message ?? "Impossible d'enregistrer la facture.");
        this.saving.set(false);
      },
    });
  }

  private populate(invoice: Invoice): void {
    this.currentInvoice.set(invoice);
    this.form.patchValue(
      {
        clientId: invoice.clientId,
        issueDate: invoice.issueDate,
        dueDate: invoice.dueDate ?? '',
      },
      { emitEvent: false },
    );
    this.loadVehicles(invoice.clientId, '', invoice.vehicleId);
    invoice.items.forEach((item) => this.addItem(item));
  }

  private ensureClient(clientId: string): void {
    if (this.clients().some((client) => client.id === clientId)) return;
    this.clientsApi.findById(clientId).subscribe({
      next: (client) => this.clients.update((clients) => [client, ...clients]),
    });
  }

  private loadVehicles(clientId: string, search = '', selectedVehicleId?: string): void {
    this.vehiclesApi.findAll(0, 20, search, clientId).subscribe({
      next: (page) => {
        this.vehicles.set(page.content);
        if (!selectedVehicleId) return;
        if (page.content.some((vehicle) => vehicle.id === selectedVehicleId)) {
          this.form.controls.vehicleId.setValue(selectedVehicleId, { emitEvent: false });
          return;
        }
        this.vehiclesApi.findById(selectedVehicleId).subscribe({
          next: (vehicle) => {
            this.vehicles.update((vehicles) => [vehicle, ...vehicles]);
            this.form.controls.vehicleId.setValue(selectedVehicleId, { emitEvent: false });
          },
        });
      },
      error: () => this.error.set('Impossible de rechercher les véhicules.'),
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
