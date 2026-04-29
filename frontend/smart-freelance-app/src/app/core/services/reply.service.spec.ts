import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReplyService } from './reply.service';

describe('ReplyService', () => {
  let service: ReplyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReplyService],
    });
    service = TestBed.inject(ReplyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('performs CRUD requests on reply endpoints', () => {
    service.create({ ticketId: 10, message: 'hello' }).subscribe();
    let req = httpMock.expectOne((r) => r.url.includes('/ticket/replies'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1 });

    service.getByTicketId(10).subscribe();
    req = httpMock.expectOne((r) => r.url.includes('/ticket/replies/10'));
    expect(req.request.method).toBe('GET');
    req.flush([]);

    service.update(1, { message: 'updated' }).subscribe();
    req = httpMock.expectOne((r) => r.url.includes('/ticket/replies/1'));
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, message: 'updated' });

    service.delete(1).subscribe();
    req = httpMock.expectOne((r) => r.url.includes('/ticket/replies/1'));
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
