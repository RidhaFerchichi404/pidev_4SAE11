import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MeetingService } from './meeting.service';
import { UserService } from './user.service';
import { ProjectApplicationService } from './project-application.service';
import { ProjectService } from './project.service';
import { AiModelStatusService } from './aimodel-status.service';

describe('Coverage Boost Services', () => {
  let httpMock: HttpTestingController;
  let meeting: MeetingService;
  let users: UserService;
  let apps: ProjectApplicationService;
  let projects: ProjectService;
  let aiStatus: AiModelStatusService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    httpMock = TestBed.inject(HttpTestingController);
    meeting = TestBed.inject(MeetingService);
    users = TestBed.inject(UserService);
    apps = TestBed.inject(ProjectApplicationService);
    projects = TestBed.inject(ProjectService);
    aiStatus = TestBed.inject(AiModelStatusService);
  });

  afterEach(() => httpMock.verify());

  it('covers meeting service endpoints and fallbacks', () => {
    meeting.getMyMeetings().subscribe((v) => expect(v).toEqual([]));
    httpMock.expectOne((r) => r.url.includes('/meeting/api/meetings') && r.method === 'GET')
      .flush({}, { status: 500, statusText: 'err' });

    meeting.getById(1).subscribe((v) => expect(v).toBeNull());
    httpMock.expectOne((r) => r.url.includes('/meeting/api/meetings/1')).flush({}, { status: 404, statusText: 'nf' });

    meeting.getStats().subscribe((s) => expect(s.total).toBe(0));
    httpMock.expectOne((r) => r.url.includes('/meeting/api/meetings/stats')).flush({}, { status: 500, statusText: 'err' });

    meeting.accept(1).subscribe();
    meeting.decline(1, 'no').subscribe();
    meeting.cancel(1, 'later').subscribe();
    meeting.complete(1).subscribe();
    meeting.generateSummary(1).subscribe();
    meeting.addComment(1, 2, 'U', 'hi').subscribe();
    const reqs = httpMock.match((r) => r.url.includes('/meeting/api/meetings/'));
    expect(reqs.length).toBeGreaterThan(5);
    reqs.forEach((r) => r.flush({}));
  });

  it('covers user service success and error mapping', () => {
    const file = new File(['x'], 'a.png', { type: 'image/png' });
    users.uploadAvatar(file).subscribe((v) => expect(v).toBeNull());
    httpMock.expectOne((r) => r.url.includes('/user/api/users/avatar')).flush({}, { status: 500, statusText: 'err' });

    users.getAll().subscribe((v) => expect(v).toEqual([]));
    httpMock.expectOne((r) => r.url.endsWith('/user/api/users')).flush({}, { status: 500, statusText: 'err' });

    users.getByEmail('a+b@x.tn').subscribe((v) => expect(v).toBeNull());
    httpMock.expectOne((r) => r.url.includes('/user/api/users/email/')).flush({}, { status: 404, statusText: 'nf' });

    users.delete(9).subscribe((ok) => expect(ok).toBeFalse());
    httpMock.expectOne((r) => r.url.endsWith('/user/api/users/9') && r.method === 'DELETE')
      .flush({}, { status: 500, statusText: 'err' });
  });

  it('covers project application service mappings', () => {
    apps.addApplication({ projectId: 1, freelanceId: 2 }).subscribe((v) => expect(v).toBeNull());
    httpMock.expectOne((r) => r.url.includes('/project/applications') && r.method === 'POST').flush({}, { status: 500, statusText: 'err' });

    apps.updateStatus(5, 'ACCEPTED').subscribe((v) => expect(v).toBeNull());
    httpMock.expectOne((r) => r.url.includes('/project/applications/5/status') && r.params.get('status') === 'ACCEPTED')
      .flush({}, { status: 500, statusText: 'err' });

    apps.deleteApplication(8).subscribe((ok) => expect(ok).toBeFalse());
    httpMock.expectOne((r) => r.url.includes('/project/applications/8') && r.method === 'DELETE')
      .flush({}, { status: 500, statusText: 'err' });
  });

  it('covers project service and ai status service', () => {
    projects.getByFreelancerId(2).subscribe((v) => expect(v).toEqual([]));
    httpMock.expectOne((r) => r.url.includes('/project/projects/recommended') && r.params.get('userId') === '2')
      .flush({}, { status: 500, statusText: 'err' });

    projects.createProject({ title: 'x', description: 'y' }).subscribe((v) => expect(v).toBeNull());
    httpMock.expectOne((r) => r.url.includes('/project/projects/add')).flush({}, { status: 500, statusText: 'err' });

    projects.deleteProject(6).subscribe((ok) => expect(ok).toBeFalse());
    httpMock.expectOne((r) => r.url.endsWith('/project/projects/6') && r.method === 'DELETE')
      .flush({}, { status: 500, statusText: 'err' });

    aiStatus.getLiveStatus().subscribe((s) => {
      expect(s.snapshot).toBeNull();
      expect(s.reachabilityError).toBeTrue();
    });
    httpMock.expectOne((r) => r.url.includes('/aimodel/api/ai/status')).flush({}, { status: 503, statusText: 'down' });
  });

  it('covers additional success branches', () => {
    users.getAll().subscribe((v) => expect(v.length).toBe(1));
    users.getByEmail('ok@x.tn').subscribe((v) => expect(v?.email).toBe('ok@x.tn'));
    apps.getAllApplications().subscribe((v) => expect(v).toEqual([]));
    aiStatus.getLiveStatus().subscribe((s) => {
      expect(s.reachabilityError).toBeFalse();
      expect(s.snapshot?.service).toBe('aimodel');
    });

    const reqs = httpMock.match(() => true);
    reqs.forEach((r) => {
      if (r.request.url.includes('/aimodel/api/ai/status')) {
        r.flush({ service: 'aimodel', status: 'UP', ollamaReachable: true, model: 'x', modelReady: true });
      } else if (r.request.url.includes('/user/api/users/email/')) {
        r.flush({ id: 1, email: 'ok@x.tn', firstName: 'Ok', lastName: 'User', role: 'CLIENT' });
      } else if (r.request.url.endsWith('/user/api/users')) {
        r.flush([{ id: 1, email: 'ok@x.tn', firstName: 'Ok', lastName: 'User', role: 'CLIENT' }]);
      } else {
        r.flush([]);
      }
    });
  });
});
