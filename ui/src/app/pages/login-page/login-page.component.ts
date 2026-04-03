import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzCardModule,
    NzButtonModule,
    NzFormModule,
    NzInputModule,
    NzAlertModule
  ],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css'
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly message = inject(NzMessageService);

  errorMessage = '';

  readonly loginForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  submit(): void {
    this.errorMessage = '';
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { username, password } = this.loginForm.getRawValue();

    this.authService.login(username, password).subscribe({
      next: () => {
        this.message.success('Autentificare reusita.');
        this.router.navigate(['/stocks']);
      },
      error: (error: unknown) => {
        this.errorMessage = this.resolveAuthError(error);
      }
    });
  }

  private resolveAuthError(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Autentificarea a esuat. Incearca din nou.';
    }

    if (error.status === 0) {
      return 'Serverul de autentificare nu raspunde. Verifica daca Keycloak ruleaza pe portul 8082.';
    }

    if (error.status === 404) {
      return 'Endpointul de autentificare nu a fost gasit. Verifica proxy-ul frontend.';
    }

    if (error.status === 401 || error.status === 400) {
      return 'Utilizator sau parola invalida.';
    }

    return `Autentificarea a esuat cu status ${error.status}.`;
  }
}

