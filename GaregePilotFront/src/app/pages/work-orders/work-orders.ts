import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Pagination } from '../../core/components/pagination/pagination';
import { Vehicle } from '../../core/models/vehicle.model';
import {
  WorkOrder,
  WorkOrderItemRequest,
  WorkOrderItemType,
  WorkOrderRequest,
  WorkOrderStatus,
} from '../../core/models/work-order.model';
import { VehicleApiService } from '../../core/services/vehicle-api.service';
import { WorkOrderApiService } from '../../core/services/work-order-api.service';

@Component({
  selector: 'app-work-orders',
  imports: [CurrencyPipe, DatePipe, Pagination, ReactiveFormsModule],
  templateUrl: './work-orders.html',
  styleUrl: './work-orders.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrders implements OnInit {
  private readonly api = inject(WorkOrderApiService);
  private readonly vehicleApi = inject(VehicleApiService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly workOrders = signal<WorkOrder[]>([]);
  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly selected = signal<WorkOrder | null>(null);
  protected readonly drawerOpen = signal(false);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalElements = signal(0);
  protected readonly search = signal('');
  protected readonly statusFilter = signal<WorkOrderStatus | undefined>(undefined);
  protected readonly vehicleSearch = signal('');
  private searchTimer?: ReturnType<typeof setTimeout>;
  private vehicleSearchTimer?: ReturnType<typeof setTimeout>;

  protected readonly form = this.formBuilder.nonNullable.group({
    vehicleId: ['', Validators.required],
    clientId: ['', Validators.required],
    mileageIn: [null as number | null, [Validators.min(0)]],
    mileageOut: [null as number | null, [Validators.min(0)]],
    description: ['', [Validators.required, Validators.maxLength(4000)]],
    technicianNotes: ['', Validators.maxLength(4000)],
    customerNotes: ['', Validators.maxLength(4000)],
    items: this.formBuilder.array([this.createItemForm()]),
  });

  protected subtotal(): number {
    return this.itemValues().reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
  }

  protected vatAmount(): number {
    return this.itemValues().reduce(
      (sum, item) => sum + item.quantity * item.unitPrice * (item.vatRate / 100),
      0,
    );
  }

  protected total(): number {
    return this.subtotal() + this.vatAmount();
  }

  ngOnInit(): void {
    this.loadVehicles();
    this.load();
  }

  protected get items(): FormArray {
    return this.form.controls.items;
  }

  protected openCreate(): void {
    this.selected.set(null);
    this.form.reset({
      vehicleId: '',
      clientId: '',
      mileageIn: null,
      mileageOut: null,
      description: '',
      technicianNotes: '',
      customerNotes: '',
    });
    this.items.clear();
    this.items.push(this.createItemForm());
    this.drawerOpen.set(true);
  }

  protected openEdit(workOrder: WorkOrder): void {
    this.selected.set(workOrder);
    this.form.reset({
      vehicleId: workOrder.vehicleId,
      clientId: workOrder.clientId,
      mileageIn: workOrder.mileageIn ?? null,
      mileageOut: workOrder.mileageOut ?? null,
      description: workOrder.description,
      technicianNotes: workOrder.technicianNotes ?? '',
      customerNotes: workOrder.customerNotes ?? '',
    });
    this.items.clear();
    workOrder.items.forEach((item) => this.items.push(this.createItemForm(item)));
    if (!workOrder.items.length) this.items.push(this.createItemForm());
    this.drawerOpen.set(true);
  }

  protected closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  protected addItem(type: WorkOrderItemType = 'SERVICE'): void {
    this.items.push(this.createItemForm({ type }));
  }

  protected removeItem(index: number): void {
    if (this.items.length > 1) this.items.removeAt(index);
  }

  protected vehicleChanged(vehicleId: string): void {
    const vehicle = this.vehicles().find((candidate) => candidate.id === vehicleId);
    this.form.controls.clientId.setValue(vehicle?.clientId ?? '');
    if (vehicle?.mileage != null && this.form.controls.mileageIn.value == null) {
      this.form.controls.mileageIn.setValue(vehicle.mileage);
    }
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const request: WorkOrderRequest = {
      clientId: raw.clientId,
      vehicleId: raw.vehicleId,
      mileageIn: raw.mileageIn ?? undefined,
      mileageOut: raw.mileageOut ?? undefined,
      description: raw.description,
      technicianNotes: raw.technicianNotes || undefined,
      customerNotes: raw.customerNotes || undefined,
      items: raw.items,
    };
    this.saving.set(true);
    this.error.set(null);
    const selected = this.selected();
    const operation = selected ? this.api.update(selected.id, request) : this.api.create(request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.closeDrawer();
        this.load();
      },
      error: () => this.error.set('L’intervention n’a pas pu être enregistrée.'),
    });
  }

  protected changeStatus(workOrder: WorkOrder, status: WorkOrderStatus): void {
    this.error.set(null);
    this.api.updateStatus(workOrder.id, status).subscribe({
      next: (updated) =>
        this.workOrders.update((current) =>
          current.map((item) => (item.id === updated.id ? updated : item)),
        ),
      error: () => this.error.set('Le statut de l’intervention n’a pas pu être modifié.'),
    });
  }

  protected changePage(page: number): void {
    this.load(page, this.pageSize());
  }

  protected changePageSize(pageSize: number): void {
    this.load(0, pageSize);
  }

  protected setSearch(search: string): void {
    this.search.set(search);
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.load(0, this.pageSize()), 300);
  }

  protected setStatusFilter(status: string): void {
    this.statusFilter.set((status || undefined) as WorkOrderStatus | undefined);
    this.load(0, this.pageSize());
  }

  protected searchVehicles(search: string): void {
    this.vehicleSearch.set(search);
    clearTimeout(this.vehicleSearchTimer);
    this.vehicleSearchTimer = setTimeout(() => this.loadVehicles(), 300);
  }

  protected statusLabel(status: WorkOrderStatus): string {
    return {
      DRAFT: 'Brouillon',
      WAITING_FOR_APPROVAL: 'Accord client',
      APPROVED: 'Approuvée',
      IN_PROGRESS: 'En cours',
      WAITING_FOR_PARTS: 'Attente pièces',
      COMPLETED: 'Terminée',
      INVOICED: 'Facturée',
      CANCELLED: 'Annulée',
    }[status];
  }

  protected itemTypeLabel(type: WorkOrderItemType): string {
    return { LABOR: 'Main-d’œuvre', PART: 'Pièce', SERVICE: 'Prestation', OTHER: 'Autre' }[type];
  }

  protected canEdit(workOrder: WorkOrder): boolean {
    return !['INVOICED', 'CANCELLED'].includes(workOrder.status);
  }

  private load(page = this.page(), pageSize = this.pageSize()): void {
    this.loading.set(true);
    this.api
      .findAll(page, pageSize, this.search(), this.statusFilter())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.workOrders.set(response.content);
          this.page.set(response.number);
          this.pageSize.set(response.size);
          this.totalElements.set(response.totalElements);
        },
        error: () => this.error.set('Impossible de charger les interventions.'),
      });
  }

  private loadVehicles(): void {
    this.vehicleApi.findAll(0, 20, this.vehicleSearch()).subscribe({
      next: (vehicles) => this.vehicles.set(vehicles.content),
      error: () => this.error.set('Impossible de charger les clients et les véhicules.'),
    });
  }

  private createItemForm(item?: Partial<WorkOrderItemRequest>) {
    return this.formBuilder.nonNullable.group({
      type: [(item?.type ?? 'SERVICE') as WorkOrderItemType, Validators.required],
      description: [item?.description ?? '', [Validators.required, Validators.maxLength(255)]],
      quantity: [item?.quantity ?? 1, [Validators.required, Validators.min(0.01)]],
      unitPrice: [item?.unitPrice ?? 0, [Validators.required, Validators.min(0)]],
      vatRate: [item?.vatRate ?? 21, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
  }

  private itemValues(): WorkOrderItemRequest[] {
    return this.items.getRawValue();
  }
}
