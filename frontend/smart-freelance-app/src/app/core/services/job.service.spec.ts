import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobService } from './job.service';

describe('JobService', () => {
  let service: JobService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [JobService],
    });
    service = TestBed.inject(JobService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('searchJobs maps filters into query params', () => {
    service.searchJobs({ keyword: 'angular', category: 'WEB', budgetMin: 100, budgetMax: 500, skillId: 3 }).subscribe();
    const req = httpMock.expectOne(
      (r) =>
        r.url.includes('/freelancia-job/jobs/search') &&
        r.params.get('keyword') === 'angular' &&
        r.params.get('category') === 'WEB' &&
        r.params.get('budgetMin') === '100' &&
        r.params.get('budgetMax') === '500' &&
        r.params.get('skillId') === '3'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('filterJobs returns server page and handles fallback', () => {
    let page: any;
    service.filterJobs({ page: 0, size: 9 }).subscribe((v) => (page = v));
    let req = httpMock.expectOne((r) => r.url.includes('/freelancia-job/jobs/filter'));
    expect(req.request.method).toBe('POST');
    req.flush({ content: [{ id: 1 }], totalElements: 1, totalPages: 1, number: 0, size: 9, first: true, last: true, empty: false });
    expect(page.content.length).toBe(1);

    service.filterJobs({ page: 1, size: 5 }).subscribe((v) => (page = v));
    req = httpMock.expectOne((r) => r.url.includes('/freelancia-job/jobs/filter'));
    req.flush({}, { status: 500, statusText: 'Server Error' });
    expect(page.content).toEqual([]);
    expect(page.size).toBe(5);
  });

  it('deleteJob maps response status to boolean', () => {
    let ok = false;
    service.deleteJob(9).subscribe((v) => (ok = v));
    const req = httpMock.expectOne((r) => r.url.includes('/freelancia-job/jobs/9'));
    expect(req.request.method).toBe('DELETE');
    req.flush({}, { status: 204, statusText: 'No Content' });
    expect(ok).toBeTrue();
  });
});
