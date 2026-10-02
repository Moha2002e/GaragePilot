import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  signal,
  ViewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  CalendarOptions,
  DateClickInfo,
  DatesSetInfo,
  EventClickInfo,
  FullCalendarComponent,
  FullCalendarModule,
} from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/angular/daygrid';
import interactionPlugin from '@fullcalendar/angular/interaction';
import listPlugin from '@fullcalendar/angular/list';
import frLocale from 'fullcalendar/locales/fr';
import timeGridPlugin from '@fullcalendar/angular/timegrid';
import classicThemePlugin from '@fullcalendar/angular/themes/classic';
import { Subject, debounceTime, distinctUntilChanged, forkJoin, switchMap } from 'rxjs';
import { Appointment, AppointmentStatus } from '../../core/models/appointment.model';
import { Client } from '../../core/models/client.model';
import { GarageService } from '../../core/models/garage-service.model';
import { Vehicle } from '../../core/models/vehicle.model';
import { AppointmentApiService } from '../../core/services/appointment-api.service';
import { ClientApiService } from '../../core/services/client-api.service';
import { GarageServiceApiService } from '../../core/services/garage-service-api.service';
import { VehicleApiService } from '../../core/services/vehicle-api.service';

@Component({
  selector: 'app-planning',
  imports: [FullCalendarModule, ReactiveFormsModule],
  templateUrl: './planning.html',
  styleUrl: './planning.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Planning {
  @ViewChild(FullCalendarComponent) private calendar?: FullCalendarComponent;
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly appointmentsApi = inject(AppointmentApiService);
  private readonly clientsApi = inject(ClientApiService);
  private readonly vehiclesApi = inject(VehicleApiService);
  private readonly servicesApi = inject(GarageServiceApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly appointments = signal<Appointment[]>([]);
  protected readonly clients = signal<Client[]>([]);
  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly services = signal<GarageService[]>([]);
  protected readonly panelOpen = signal(false);
  protected readonly selectedAppointment = signal<Appointment | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly clientSearch$ = new Subject<string>();
  private readonly vehicleSearch$ = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    clientId: ['', Validators.required],
    vehicleId: ['', Validators.required],
    serviceId: [''],
    reason: ['', Validators.required],
    startDateTime: ['', Validators.required],
    endDateTime: ['', Validators.required],
    status: ['PLANNED' as AppointmentStatus, Validators.required],
    notes: [''],
  });

  protected readonly calendarOptions: CalendarOptions = {
    plugins: [classicThemePlugin, dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin],
    locale: frLocale,
    initialView: 'timeGridWeek',
    firstDay: 1,
    height: 'auto',
    nowIndicator: true,
    selectable: true,
    allDaySlot: false,
    slotMinTime: '07:00:00',
    slotMaxTime: '20:00:00',
    businessHours: { daysOfWeek: [1, 2, 3, 4, 5, 6], startTime: '08:00', endTime: '18:30' },
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek',
    },
    events: [],
    datesSet: (info) => this.loadRange(info),
    dateClick: (info) => this.openCreate(info),
    eventClick: (info) => this.openEdit(info),
  };

  constructor() {
    forkJoin({
      clients: this.clientsApi.findAll(0, 20),
      services: this.servicesApi.findAll(),
    }).subscribe({
      next: ({ clients, services }) => {
        this.clients.set(clients.content);
        this.services.set(services);
      },
      error: () => this.error.set('Certaines données nécessaires au planning sont indisponibles.'),
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

  protected clientName(client: Client): string {
    return client.type === 'COMPANY'
      ? client.companyName || 'Société sans nom'
      : `${client.firstName ?? ''} ${client.lastName ?? ''}`.trim();
  }

  protected availableVehicles(): Vehicle[] {
    const clientId = this.form.controls.clientId.value;
    return this.vehicles().filter((vehicle) => !clientId || vehicle.clientId === clientId);
  }

  protected openEmpty(): void {
    this.resetForm(new Date());
    this.panelOpen.set(true);
  }

  protected closePanel(): void {
    this.panelOpen.set(false);
    this.selectedAppointment.set(null);
  }

  protected serviceChanged(): void {
    const service = this.services().find((item) => item.id === this.form.controls.serviceId.value);
    if (!service) return;

    this.form.controls.reason.setValue(service.name);
    const start = new Date(this.form.controls.startDateTime.value);
    if (!Number.isNaN(start.getTime())) {
      this.form.controls.endDateTime.setValue(
        this.toLocalInput(new Date(start.getTime() + service.durationMinutes * 60_000)),
      );
    }
  }

  protected clientChanged(): void {
    const clientId = this.form.controls.clientId.value;
    this.form.controls.vehicleId.setValue('');
    this.vehicles.set([]);
    if (clientId) this.loadVehicles(clientId);
  }

  protected searchClients(search: string): void {
    this.clientSearch$.next(search);
  }

  protected searchVehicles(search: string): void {
    if (!this.form.controls.clientId.value) return;
    this.vehicleSearch$.next(search);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request = {
      clientId: value.clientId,
      vehicleId: value.vehicleId,
      reason: value.reason,
      startDateTime: new Date(value.startDateTime).toISOString(),
      endDateTime: new Date(value.endDateTime).toISOString(),
      status: value.status,
      notes: value.notes || undefined,
    };
    if (new Date(request.endDateTime) <= new Date(request.startDateTime)) {
      this.error.set('L’heure de fin doit être postérieure à l’heure de début.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const selected = this.selectedAppointment();
    const operation = selected
      ? this.appointmentsApi.update(selected.id, request)
      : this.appointmentsApi.create(request);
    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closePanel();
        this.refreshCurrentRange();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Le rendez-vous n’a pas pu être enregistré.');
      },
    });
  }

  protected statusLabel(status: AppointmentStatus): string {
    return {
      PLANNED: 'Planifié',
      CONFIRMED: 'Confirmé',
      IN_PROGRESS: 'En cours',
      COMPLETED: 'Terminé',
      CANCELLED: 'Annulé',
      NO_SHOW: 'Absent',
    }[status];
  }

  private loadRange(info: DatesSetInfo): void {
    this.loading.set(true);
    this.appointmentsApi.findRange(info.start.toISOString(), info.end.toISOString()).subscribe({
      next: (appointments) => {
        this.appointments.set(appointments);
        this.calendarOptions.events = appointments.map((appointment) => ({
          id: appointment.id,
          title: `${appointment.licensePlate} · ${appointment.reason}`,
          start: appointment.startDateTime,
          end: appointment.endDateTime,
          backgroundColor: this.statusColor(appointment.status),
          borderColor: this.statusColor(appointment.status),
          extendedProps: { appointment },
        }));
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger les rendez-vous.');
      },
    });
  }

  private openCreate(info: DateClickInfo): void {
    this.resetForm(info.date);
    this.panelOpen.set(true);
  }

  private openEdit(info: EventClickInfo): void {
    const appointment = info.event.extendedProps['appointment'] as Appointment;
    this.selectedAppointment.set(appointment);
    this.ensureClient(appointment.clientId);
    this.loadVehicles(appointment.clientId, '', appointment.vehicleId);
    this.form.reset({
      clientId: appointment.clientId,
      vehicleId: '',
      serviceId: '',
      reason: appointment.reason,
      startDateTime: this.toLocalInput(new Date(appointment.startDateTime)),
      endDateTime: this.toLocalInput(new Date(appointment.endDateTime)),
      status: appointment.status,
      notes: appointment.notes ?? '',
    });
    this.panelOpen.set(true);
  }

  private resetForm(date: Date): void {
    const start = new Date(date);
    if (start.getHours() === 0) start.setHours(8, 0, 0, 0);
    const end = new Date(start.getTime() + 60 * 60_000);
    this.selectedAppointment.set(null);
    this.vehicles.set([]);
    this.form.reset({
      clientId: '',
      vehicleId: '',
      serviceId: '',
      reason: '',
      startDateTime: this.toLocalInput(start),
      endDateTime: this.toLocalInput(end),
      status: 'PLANNED',
      notes: '',
    });
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
          this.form.controls.vehicleId.setValue(selectedVehicleId);
          return;
        }
        this.vehiclesApi.findById(selectedVehicleId).subscribe({
          next: (vehicle) => {
            this.vehicles.update((vehicles) => [vehicle, ...vehicles]);
            this.form.controls.vehicleId.setValue(selectedVehicleId);
          },
        });
      },
      error: () => this.error.set('Impossible de rechercher les véhicules.'),
    });
  }

  private refreshCurrentRange(): void {
    const view = this.calendar?.getApi().view;
    if (view) {
      this.loadRange({ start: view.activeStart, end: view.activeEnd } as DatesSetInfo);
    }
  }

  private toLocalInput(date: Date): string {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
  }

  private statusColor(status: AppointmentStatus): string {
    return {
      PLANNED: '#6172f3',
      CONFIRMED: '#087a55',
      IN_PROGRESS: '#f79009',
      COMPLETED: '#12b76a',
      CANCELLED: '#98a2b3',
      NO_SHOW: '#f04438',
    }[status];
  }
}
