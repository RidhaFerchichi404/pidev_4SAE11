import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NotificationService],
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getByUserId returns list and normalizes non-arrays', () => {
    let list: any;
    service.getByUserId(8).subscribe((v) => (list = v));
    const req = httpMock.expectOne((r) => r.url.includes('/notification/api/notifications/user/8'));
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'n1', read: false }]);
    expect(list.length).toBe(1);

    service.getByUserId('x').subscribe((v) => (list = v));
    const req2 = httpMock.expectOne((r) => r.url.includes('/notification/api/notifications/user/x'));
    req2.flush({ bad: true });
    expect(list).toEqual([]);
  });

  it('markRead and delete map success/failure states', () => {
    let marked: any;
    service.markRead('n1').subscribe((v) => (marked = v));
    const patch = httpMock.expectOne((r) => r.url.includes('/notification/api/notifications/n1/read'));
    expect(patch.request.method).toBe('PATCH');
    patch.flush({ id: 'n1', read: true });
    expect(marked.read).toBeTrue();

    let deleted = false;
    service.delete('n2').subscribe((v) => (deleted = v));
    const del = httpMock.expectOne((r) => r.url.includes('/notification/api/notifications/n2'));
    expect(del.request.method).toBe('DELETE');
    del.flush({}, { status: 204, statusText: 'No Content' });
    expect(deleted).toBeTrue();
  });

  it('getNotificationRoute selects dashboard route by type', () => {
    expect(service.getNotificationRoute({ type: 'TASK_STATUS_UPDATE', data: { projectId: '11' } } as any, true)).toEqual({
      route: '/dashboard/project-tasks',
      queryParams: { projectId: '11' },
    });
    expect(service.getNotificationRoute({ type: 'REVIEW_RESPONSE', data: { reviewId: '7' } } as any, false)).toEqual({
      route: '/dashboard/reviews/about-me',
      queryParams: { reviewId: '7' },
    });
    expect(service.getNotificationRoute({ type: 'GAMIFICATION_TOP_FREELANCER' } as any, false).route).toContain(
      '/dashboard/gamification'
    );
  });
});
