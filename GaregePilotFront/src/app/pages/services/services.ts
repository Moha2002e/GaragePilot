import { CurrencyPipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  GarageService,
  GarageServiceRequest,
  ServiceCategory,
} from '../../core/models/garage-service.model';
import { GarageServiceApiService } from '../../core/services/garage-service-api.service';

@Component({
  selector: 'app-services',
  imports: [CurrencyPipe, DecimalPipe, ReactiveFormsModule],
  templateUrl: './services.html',
  styleUrl: './services.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Services implements OnInit {
  private readonly api = inject(GarageServiceApiService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly services = signal<GarageService[]>([]);
  protected readonly search = signal('');
  protected readonly selectedCategory = signal<ServiceCategory | 'ALL'>('ALL');
  protected readonly dialogOpen = signal(false);
  protected readonly selected = signal<GarageService | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  private filterTimer?: ReturnType<typeof setTimeout>;
  protected readonly averageDuration = computed(() => {
    const services = this.services();
    return services.length
      ? services.reduce((sum, service) => sum + service.durationMinutes, 0) / services.length
      : 0;
  });
  protected readonly averagePrice = computed(() => {
    const services = this.services();
    return services.length
      ? services.reduce((sum, service) => sum + service.price, 0) / services.length
      : null;
  });

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    category: ['MAINTENANCE' as ServiceCategory, Validators.required],
    description: [''],
    durationMinutes: [60, [Validators.required, Validators.min(1)]],
    price: [0, [Validators.required, Validators.min(0)]],
    vatRate: [21, [Validators.required, Validators.min(0)]],
    color: ['#087a55'],
  });

  ngOnInit(): void {
    this.load();
  }

  protected openCreate(): void {
    this.selected.set(null);
    this.form.reset({
      name: '',
      category: 'MAINTENANCE',
      description: '',
      durationMinutes: 60,
      price: 0,
      vatRate: 21,
      color: '#087a55',
    });
    this.dialogOpen.set(true);
  }

  protected openEdit(service: GarageService): void {
    this.selected.set(service);
    this.form.reset({
      name: service.name,
      category: service.category,
      description: service.description ?? '',
      durationMinutes: service.durationMinutes,
      price: service.price,
      vatRate: service.vatRate,
      color: service.color ?? '#087a55',
    });
    this.dialogOpen.set(true);
  }

  protected closeDialog(): void {
    this.dialogOpen.set(false);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const request: GarageServiceRequest = this.form.getRawValue();
    const selected = this.selected();
    const operation = selected ? this.api.update(selected.id, request) : this.api.create(request);
    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeDialog();
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('La prestation n’a pas pu être enregistrée.');
      },
    });
  }

  protected deactivate(service: GarageService): void {
    if (!window.confirm(`Désactiver la prestation « ${service.name} » ?`)) return;
    this.api.deactivate(service.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('La prestation n’a pas pu être désactivée.'),
    });
  }

  protected categoryLabel(category: ServiceCategory): string {
    return {
      MAINTENANCE: 'Entretien',
      MECHANICAL: 'Mécanique',
      TIRES: 'Pneumatiques',
      DIAGNOSTIC: 'Diagnostic',
      BODYWORK: 'Carrosserie',
      ELECTRICAL: 'Électricité',
      OTHER: 'Autre',
    }[category];
  }

  protected durationLabel(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const remaining = minutes % 60;
    return hours ? `${hours} h${remaining ? ` ${remaining} min` : ''}` : `${remaining} min`;
  }

  protected setSearch(search: string): void {
    this.search.set(search);
    this.scheduleFilter();
  }

  protected setCategory(category: ServiceCategory | 'ALL'): void {
    this.selectedCategory.set(category);
    this.load();
  }

  private scheduleFilter(): void {
    clearTimeout(this.filterTimer);
    this.filterTimer = setTimeout(() => this.load(), 300);
  }

  private load(): void {
    this.loading.set(true);
    const category = this.selectedCategory();
    this.api.findAll(this.search(), category === 'ALL' ? undefined : category).subscribe({
      next: (services) => {
        this.services.set(services);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le catalogue de prestations.');
        this.loading.set(false);
      },
    });
  }
}
