import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Pagination } from '../../core/components/pagination/pagination';
import { ClientStateService } from './client-state.service';

@Component({
  selector: 'app-clients',
  imports: [Pagination],
  providers: [ClientStateService],
  templateUrl: './clients.html',
  styleUrl: './clients.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Clients implements OnInit {
  protected readonly state = inject(ClientStateService);
  ngOnInit(): void {
    this.state.load();
  }
}
