import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProjectService } from './project.service';

describe('ProjectService extra coverage', () => {
  let service: ProjectService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ProjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('covers project API success and fallback endpoints', () => {
    service.getById(1).subscribe((v) => expect(v).toBeNull());
    httpMock.expectOne((r) => r.url.includes('/project/projects/1')).flush({}, { status: 404, statusText: 'nf' });

    service.getByClientId(5).subscribe((v) => expect(v).toEqual([]));
    service.getProjectsAsClientWithAcceptedFreelancer(5).subscribe((v) => expect(v).toEqual([]));
    service.getProjectsForFreelancer(7).subscribe((v) => expect(v).toEqual([]));
    service.getApplicationsByFreelancer(7).subscribe((v) => expect(v).toEqual([]));
    service.exportProjectsPdf().subscribe((v) => expect(v).toBeTruthy());
    service.getAllProjects().subscribe();
    service.getProjectById('2').subscribe((v) => expect(v).toBeNull());
    service.createProject({ title: 'a', description: 'b' }).subscribe((v) => expect(v).toBeNull());
    service.updateProject({ id: 1, title: 'c', description: 'd' }).subscribe((v) => expect(v).toBeNull());
    service.deleteProject(3).subscribe((ok) => expect(ok).toBeFalse());
    service.getRecommendedProjects(9).subscribe();
    service.getProjectStatistics().subscribe();

    const reqs = httpMock.match(() => true);
    expect(reqs.length).toBeGreaterThan(10);
    reqs.forEach((r) => {
      if (r.request.responseType === 'blob') {
        r.flush(new Blob(['pdf'], { type: 'application/pdf' }));
      } else if (
        r.request.url.includes('/project/projects/client/') ||
        r.request.url.includes('/with-accepted-freelancer') ||
        r.request.url.includes('/project/projects/freelancer/') ||
        r.request.url.includes('/project/projects/list') ||
        r.request.url.includes('/project/projects/recommended') ||
        r.request.url.includes('/project/projects/statistics')
      ) {
        r.flush([]);
      } else {
        r.flush({}, { status: 500, statusText: 'err' });
      }
    });
  });
});
