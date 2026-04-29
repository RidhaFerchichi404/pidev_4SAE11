import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProjectApplicationService } from './project-application.service';

describe('ProjectApplicationService extra coverage', () => {
  let service: ProjectApplicationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ProjectApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('covers additional application endpoints', () => {
    service.getProjectApplicationStatistics().subscribe((v) => expect(v).toEqual([]));
    service.updateApplication({ id: 1, projectId: 1, freelanceId: 2 }).subscribe((v) => expect(v).toBeNull());
    service.getAllApplications().subscribe();
    service.getApplicationById(1).subscribe((v) => expect(v).toBeNull());
    service.getApplicationsByProject(1).subscribe((v) => expect(v).toEqual([]));
    service.getApplicationsByFreelance(2).subscribe((v) => expect(v).toEqual([]));

    const reqs = httpMock.match(() => true);
    expect(reqs.length).toBe(6);
    reqs.forEach((r) => {
      if (r.request.url.endsWith('/all')) {
        r.flush([]);
      } else {
        r.flush({}, { status: 500, statusText: 'err' });
      }
    });
  });
});
