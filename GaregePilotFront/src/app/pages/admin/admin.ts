import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AdminGarageDetail,
  AdminGarageSummary,
  AdminOverview,
  AdminUser,
  AdminUserRequest,
  ManagedUserRole,
} from '../../core/models/admin.model';
import { InvoiceStatus } from '../../core/models/invoice.model';
import { AdminApiService } from '../../core/services/admin-api.service';
import { Pagination } from '../../core/components/pagination/pagination';

@Component({
  selector: 'app-admin',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, Pagination],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Admin implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly overview = signal<AdminOverview | null>(null);
  protected readonly selectedGarage = signal<AdminGarageDetail | null>(null);
  protected readonly selectedUser = signal<AdminUser | null>(null);
  protected readonly loading = signal(true);
  protected readonly detailLoading = signal(false);
  protected readonly userDialogOpen = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly search = signal('');
  private searchTimer?: ReturnType<typeof setTimeout>;

  protected readonly userForm = this.formBuilder.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    role: ['RECEPTION' as ManagedUserRole, Validators.required],
    password: ['', Validators.minLength(10)],
  });

  ngOnInit(): void {
    this.loadOverview();
  }

  protected openGarage(garage: AdminGarageSummary): void {
    this.detailLoading.set(true);
    this.error.set(null);
    this.api.garage(garage.id).subscribe({
      next: (detail) => {
        this.selectedGarage.set(detail);
        this.detailLoading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger la société.');
        this.detailLoading.set(false);
      },
    });
  }

  protected closeGarage(): void {
    this.selectedGarage.set(null);
  }

  protected openCreateUser(): void {
    this.selectedUser.set(null);
    this.userForm.reset({
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      role: 'RECEPTION',
      password: '',
    });
    this.userDialogOpen.set(true);
  }

  protected openEditUser(user: AdminUser): void {
    if (user.role === 'SUPER_ADMIN') return;
    this.selectedUser.set(user);
    this.userForm.reset({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone ?? '',
      role: user.role,
      password: '',
    });
    this.userDialogOpen.set(true);
  }

  protected closeUserDialog(): void {
    this.userDialogOpen.set(false);
  }

  protected saveUser(): void {
    const garage = this.selectedGarage();
    if (!garage || this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }
    const raw = this.userForm.getRawValue();
    if (!this.selectedUser() && !raw.password) {
      this.error.set('Un mot de passe de 10 caractères minimum est obligatoire.');
      return;
    }
    const request: AdminUserRequest = { ...raw, password: raw.password || undefined };
    this.saving.set(true);
    const selected = this.selectedUser();
    const operation = selected
      ? this.api.updateUser(selected.id, request)
      : this.api.createUser(garage.id, request);
    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeUserDialog();
        this.reloadGarage(garage.id);
        this.loadOverview();
      },
      error: () => {
        this.saving.set(false);
        this.error.set('L’utilisateur n’a pas pu être enregistré. Vérifiez l’adresse email.');
      },
    });
  }

  protected toggleGarage(garage: AdminGarageDetail): void {
    const verb = garage.active ? 'désactiver' : 'réactiver';
    if (!window.confirm(`Voulez-vous ${verb} ${garage.name} ?`)) return;
    this.api.setGarageActive(garage.id, !garage.active).subscribe({
      next: () => {
        this.reloadGarage(garage.id);
        this.loadOverview();
      },
      error: () => this.error.set('Le statut de la société n’a pas pu être modifié.'),
    });
  }

  protected toggleUser(user: AdminUser): void {
    this.api.setUserActive(user.id, !user.active).subscribe({
      next: () => this.reloadGarage(this.selectedGarage()!.id),
      error: () => this.error.set('Le statut de l’utilisateur n’a pas pu être modifié.'),
    });
  }

  protected roleLabel(role: string): string {
    return (
      {
        SUPER_ADMIN: 'Plateforme',
        OWNER: 'Propriétaire',
        ADMIN: 'Administrateur',
        MECHANIC: 'Mécanicien',
        RECEPTION: 'Réception',
      }[role] ?? role
    );
  }

  protected invoiceStatusLabel(status: InvoiceStatus): string {
    return {
      DRAFT: 'Brouillon',
      ISSUED: 'Émise',
      PARTIALLY_PAID: 'Partielle',
      PAID: 'Payée',
      OVERDUE: 'En retard',
      CANCELLED: 'Annulée',
    }[status];
  }

  protected changePage(page: number): void {
    this.loadOverview(page, this.pageSize());
  }

  protected changePageSize(pageSize: number): void {
    this.loadOverview(0, pageSize);
  }

  protected setSearch(search: string): void {
    this.search.set(search);
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.loadOverview(0, this.pageSize()), 300);
  }

  private loadOverview(page = this.page(), pageSize = this.pageSize()): void {
    this.loading.set(true);
    this.api.overview(page, pageSize, this.search()).subscribe({
      next: (overview) => {
        this.overview.set(overview);
        this.page.set(overview.companies.number);
        this.pageSize.set(overview.companies.size);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('L’espace administrateur est inaccessible.');
        this.loading.set(false);
      },
    });
  }

  private reloadGarage(id: string): void {
    this.api.garage(id).subscribe({ next: (detail) => this.selectedGarage.set(detail) });
  }
}
