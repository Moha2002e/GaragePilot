import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Pagination } from '../../core/components/pagination/pagination';
import { VehicleStateService } from './vehicle-state.service';

@Component({
  selector: 'app-vehicles',
  imports: [Pagination],
  providers: [VehicleStateService],
  templateUrl: './vehicles.html',
  styleUrl: './vehicles.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Vehicles implements OnInit {
  protected readonly state = inject(VehicleStateService);
  ngOnInit(): void {
    this.state.load();
  }
}
