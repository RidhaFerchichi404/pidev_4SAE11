import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TicketService } from './ticket.service';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TicketService],
    });
    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('builds list query params for getAll', () => {
    service.getAll({ priority: 'HIGH', status: 'OPEN', q: 'payment', page: 2, size: 20 }).subscribe();
    const req = httpMock.expectOne(
      (r) =>
        r.url.includes('/ticket/tickets') &&
        r.params.get('priority') === 'HIGH' &&
        r.params.get('status') === 'OPEN' &&
        r.params.get('q') === 'payment' &&
        r.params.get('page') === '2' &&
        r.params.get('size') === '20'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ items: [], currentPage: 0, pageSize: 20, totalPages: 0, totalElements: 0 });
  });

  it('calls ticket lifecycle endpoints', () => {
    service.create({ subject: 'Need help' }).subscribe();
    let req = httpMock.expectOne((r) => r.url.endsWith('/ticket/tickets'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1 });

    service.close(1).subscribe();
    req = httpMock.expectOne((r) => r.url.endsWith('/ticket/tickets/1/close'));
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, status: 'CLOSED' });

    service.reopen(1).subscribe();
    req = httpMock.expectOne((r) => r.url.endsWith('/ticket/tickets/1/reopen'));
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, status: 'OPEN' });
  });

  it('gets blob export from pdf endpoint', () => {
    service.exportPdf().subscribe();
    const req = httpMock.expectOne((r) => r.url.endsWith('/ticket/tickets/export/pdf'));
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
