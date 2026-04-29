import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: Router, useValue: routerSpy }],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('logs in, fetches profile, stores token and user id', () => {
    const token =
      'eyJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6ImRldkBwbGF0Zm9ybS50biIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJGUkVFTEFOQ0VSIl19fQ.signature';
    let result: any;
    service.login('dev@platform.tn', 'pwd', true).subscribe((r) => (result = r));

    const loginReq = httpMock.expectOne((r) => r.url.includes('/keycloak-auth/api/auth/token'));
    expect(loginReq.request.method).toBe('POST');
    loginReq.flush({ access_token: token, refresh_token: 'rt' });

    const profileReq = httpMock.expectOne((r) => r.url.includes('/user/api/users/email/'));
    profileReq.flush({ id: 12, email: 'dev@platform.tn', firstName: 'Dev', lastName: 'User', role: 'FREELANCER' });

    expect(result.access_token).toBe(token);
    expect(service.getUserId()).toBe(12);
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('refreshes token when refresh token is available', () => {
    sessionStorage.setItem('refresh_token', 'rt');
    let out: any;
    service.refreshToken().subscribe((v) => (out = v));
    const req = httpMock.expectOne((r) => r.url.includes('/keycloak-auth/api/auth/refresh'));
    expect(req.request.method).toBe('POST');
    req.flush({ access_token: 'new-token', refresh_token: 'new-rt' });
    expect(out.access_token).toBe('new-token');
  });

  it('maps login error and logs out on refresh failure', () => {
    let loginOut: any;
    service.login('bad@x.tn', 'bad', false).subscribe((v) => (loginOut = v));
    const loginReq = httpMock.expectOne((r) => r.url.includes('/keycloak-auth/api/auth/token'));
    loginReq.flush({ error: 'invalid_grant' }, { status: 401, statusText: 'Unauthorized' });
    expect(loginOut.error).toContain('Invalid email or password');

    sessionStorage.setItem('refresh_token', 'rt');
    let refreshOut: any;
    service.refreshToken().subscribe((v) => (refreshOut = v));
    const refreshReq = httpMock.expectOne((r) => r.url.includes('/keycloak-auth/api/auth/refresh'));
    refreshReq.flush({}, { status: 500, statusText: 'Server Error' });
    expect(refreshOut.error).toContain('Session expired');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('supports forgot password and register fallback error mapping', () => {
    let forgot: any;
    service.forgotPassword('x@y.tn').subscribe((v) => (forgot = v));
    const forgotReq = httpMock.expectOne((r) => r.url.includes('/keycloak-auth/api/auth/forgot-password'));
    forgotReq.flush({ message: 'ok' });
    expect(forgot.message).toBe('ok');

    let register: any;
    service
      .register({
        email: 'a@b.tn',
        password: '12345678',
        firstName: 'A',
        lastName: 'B',
        role: 'FREELANCER',
      })
      .subscribe((v) => (register = v));
    const regReq = httpMock.expectOne((r) => r.url.includes('/keycloak-auth/api/auth/register'));
    regReq.flush({ error: 'User with email already exists' }, { status: 409, statusText: 'Conflict' });
    expect(register.error).toContain('already exists');
  });

  it('logout clears storage and redirects', fakeAsync(() => {
    localStorage.setItem('access_token', 'x');
    localStorage.setItem('refresh_token', 'y');
    (service as any).tokenSignal.set('x');
    service.logout();
    tick();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(sessionStorage.getItem('access_token')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  }));
});
