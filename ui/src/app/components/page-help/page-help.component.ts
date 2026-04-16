import { CommonModule } from '@angular/common';
import { Component, input, signal } from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';

@Component({
  selector: 'app-page-help',
  standalone: true,
  imports: [CommonModule, NzButtonModule, NzDrawerModule],
  templateUrl: './page-help.component.html',
  styleUrl: './page-help.component.css'
})
export class PageHelpComponent {
  readonly buttonLabel = input<string>('?');
  readonly title = input<string>('Ajutor');
  readonly intro = input<string>('Foloseste acest ghid pentru a intelege rapid pagina curenta.');
  readonly steps = input<string[]>([]);
  readonly tips = input<string[]>([]);

  readonly helpVisible = signal(false);

  openHelp(): void {
    this.helpVisible.set(true);
  }

  closeHelp(): void {
    this.helpVisible.set(false);
  }
}