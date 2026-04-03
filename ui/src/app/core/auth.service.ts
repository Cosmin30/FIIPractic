import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, switchMap, tap, throwError } from 'rxjs';
import { ApiService } from './api.service';
import { UserInfo } from './models';

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  refresh_expires_in: number;
  token_type: string;
  scope: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly keycloakClientId = 'stock-analyzer-app';
  private readonly keycloakRealm = 'stock-realm';

  private readonly accessTokenKey = 'stocks_access_token';
  private readonly refreshTokenKey = 'stocks_refresh_token';

  private readonly _user = signal<UserInfo | null>(null);
  private readonly _loading = signal(false);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly roles = computed(() => this._user()?.roles ?? []);

  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    if (!token) {
      return false;
    }

    if (this.isTokenExpired(token)) {
      this.clearSession();
      return false;
    }

    return true;
  }

  login(username: string, password: string): Observable<UserInfo> {
    this._loading.set(true);

    return this.requestToken(username, password)
      .pipe(
        tap((response) => {
          localStorage.setItem(this.accessTokenKey, response.access_token);
          localStorage.setItem(this.refreshTokenKey, response.refresh_token);
        }),
        switchMap(() => this.fetchCurrentUser()),
        finalize(() => this._loading.set(false))
      );
  }

  restoreSession(): Observable<UserInfo | null> {
    if (!this.getAccessToken()) {
      this._user.set(null);
      return of(null);
    }

    return this.fetchCurrentUser().pipe(catchError(() => of(null)));
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.accessTokenKey);
  }

  private fetchCurrentUser(): Observable<UserInfo> {
    return this.api.whoAmI().pipe(
      map((user) => ({ ...user, roles: user.roles ?? [] })),
      tap((user) => this._user.set(user)),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          this.clearSession();
        }

        return throwError(() => error);
      })
    );
  }

  private clearSession(): void {
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    this._user.set(null);
  }

  private isTokenExpired(token: string): boolean {
    const payload = this.decodeTokenPayload(token);
    if (!payload || typeof payload.exp !== 'number') {
      return false;
    }

    const nowEpochSeconds = Math.floor(Date.now() / 1000);
    return payload.exp <= nowEpochSeconds + 5;
  }

  private decodeTokenPayload(token: string): { exp?: number } | null {
    try {
      const parts = token.split('.');
      if (parts.length < 2) {
        return null;
      }

      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const paddingLength = (4 - (base64.length % 4)) % 4;
      const padded = base64 + '='.repeat(paddingLength);

      return JSON.parse(atob(padded)) as { exp?: number };
    } catch {
      return null;
    }
  }

  private requestToken(username: string, password: string): Observable<TokenResponse> {
    const proxiedTokenUrl = `/auth/realms/${this.keycloakRealm}/protocol/openid-connect/token`;
    const directTokenUrl = `http://localhost:8082/realms/${this.keycloakRealm}/protocol/openid-connect/token`;
    const body = new HttpParams()
      .set('client_id', this.keycloakClientId)
      .set('grant_type', 'password')
      .set('username', username)
      .set('password', password);

    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    return this.http.post<TokenResponse>(
      proxiedTokenUrl,
      body.toString(),
      { headers }
    ).pipe(
      catchError((error: HttpErrorResponse) => {
        // During local dev, this fallback helps when ng serve is started without proxy config.
        if (
          error.status === 404
          && window.location.hostname === 'localhost'
          && window.location.port === '4200'
        ) {
          return this.http.post<TokenResponse>(directTokenUrl, body.toString(), { headers });
        }

        return throwError(() => error);
      })
    );
  }
}
