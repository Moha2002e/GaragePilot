import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.html',
  styleUrl: './pagination.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pagination {
  @Input({ required: true }) totalElements = 0;
  @Input({ required: true }) page = 0;
  @Input({ required: true }) pageSize = 10;
  @Input() pageSizeOptions = [10, 20, 50];

  @Output() readonly pageChange = new EventEmitter<number>();
  @Output() readonly pageSizeChange = new EventEmitter<number>();

  protected get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements / this.pageSize));
  }

  protected get firstElement(): number {
    return this.totalElements === 0 ? 0 : this.page * this.pageSize + 1;
  }

  protected get lastElement(): number {
    return Math.min((this.page + 1) * this.pageSize, this.totalElements);
  }

  protected get visiblePages(): number[] {
    const start = Math.max(0, Math.min(this.page - 2, this.totalPages - 5));
    const end = Math.min(this.totalPages, start + 5);
    return Array.from({ length: end - start }, (_, index) => start + index);
  }

  protected selectPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.page) {
      this.pageChange.emit(page);
    }
  }

  protected selectPageSize(event: Event): void {
    const pageSize = Number((event.target as HTMLSelectElement).value);
    if (pageSize !== this.pageSize) {
      this.pageSizeChange.emit(pageSize);
    }
  }
}
