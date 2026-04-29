import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user.service';

describe('UserService extra coverage', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('covers user success mappings', () => {
    const file = new File(['x'], 'avatar.png', { type: 'image/png' });
    service.uploadAvatar(file).subscribe((v) => expect(v).toBe('/cdn/a.png'));
    service.getById(2).subscribe((v) => expect(v?.id).toBe(2));
    service.create({ email: 'a@b.tn', firstName: 'A', lastName: 'B', role: 'CLIENT' }).subscribe((v) => expect(v?.email).toContain('@'));
    service.update(2, { firstName: 'Z' }).subscribe((v) => expect(v?.firstName).toBe('Z'));
    service.delete(2).subscribe((ok) => expect(ok).toBeTrue());

    const reqs = httpMock.match(() => true);
    expect(reqs.length).toBe(5);
    reqs.forEach((r) => {
      if (r.request.url.endsWith('/avatar')) r.flush({ avatarUrl: '/cdn/a.png' });
      else if (r.request.method === 'DELETE') r.flush({});
      else r.flush({ id: 2, email: 'a@b.tn', firstName: 'Z', lastName: 'B', role: 'CLIENT' });
    });
  });
});
