import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PlanningService } from './planning.service';
import { TaskService } from './task.service';

describe('PlanningTaskCoverage', () => {
  let httpMock: HttpTestingController;
  let planning: PlanningService;
  let task: TaskService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    httpMock = TestBed.inject(HttpTestingController);
    planning = TestBed.inject(PlanningService);
    task = TestBed.inject(TaskService);
  });

  afterEach(() => httpMock.verify());

  it('covers planning service query builders and fallbacks', () => {
    planning.getFilteredProgressUpdates({ page: 1, size: 5, projectId: 2, search: 'api' }).subscribe((p) => {
      expect(p.content).toEqual([]);
    });
    httpMock.expectOne((r) => r.url.includes('/planning/api/progress-updates') && r.url.includes('search=api'))
      .flush({}, { status: 500, statusText: 'err' });

    planning.getLatestProgressUpdateByProject(1).subscribe((v) => expect(v).toBeNull());
    planning.getLatestProgressUpdateByFreelancer(2).subscribe((v) => expect(v).toBeNull());
    planning.getLatestProgressUpdateByContract(3).subscribe((v) => expect(v).toBeNull());
    httpMock.match((r) => r.url.includes('/progress-updates/latest')).forEach((r) => r.flush({}, { status: 404, statusText: 'nf' }));

    planning.validateProgressUpdate({ projectId: 1, contractId: null, freelancerId: 2, title: 't', progressPercentage: 20 }).subscribe((v) => {
      expect(v.valid).toBeFalse();
    });
    httpMock.expectOne((r) => r.url.includes('/progress-updates/validate')).flush({}, { status: 500, statusText: 'err' });

    planning.getCalendarEvents({ userId: 7, role: 'FREELANCER' }).subscribe((rows) => expect(rows).toEqual([]));
    planning.getGitHubBranches('owner', 'repo').subscribe((rows) => expect(rows).toEqual([]));
    planning.getGitHubLatestCommit('owner', 'repo', 'main').subscribe((v) => expect(v).toBeNull());
    planning.getGitHubCommits('owner', 'repo', 'main', 10).subscribe((rows) => expect(rows).toEqual([]));
    planning.createGitHubIssue('owner', 'repo', 'bug').subscribe((v) => expect(v).toBeNull());
    httpMock.match(() => true).forEach((r) => r.flush({}, { status: 500, statusText: 'err' }));
  });

  it('covers task service endpoints and helper behavior', () => {
    task.getFilteredTasks({ page: 0, size: 10, assigneeId: 2, openTasksOnly: true }).subscribe({ error: () => {} });
    httpMock.expectOne((r) => r.url.includes('/task/api/tasks?') && r.url.includes('openTasksOnly=true'))
      .flush({}, { status: 500, statusText: 'err' });

    task.getSubtaskProgress(2, []).subscribe((v) => expect(v).toEqual({}));

    task.getSubtaskProgress(2, [1, 2]).subscribe((v) => expect(v[1].completed).toBe(1));
    httpMock.expectOne((r) => r.url.includes('/subtask-progress')).flush([{ parentTaskId: 1, total: 3, completed: 1 }]);

    task.patchAssignee(2, null).subscribe((v) => expect(v).toBeNull());
    task.patchSubtaskDueDate(3, '').subscribe((v) => expect(v).toBeNull());
    task.deleteTask(1).subscribe((ok) => expect(ok).toBeFalse());
    task.deleteComment(1).subscribe((ok) => expect(ok).toBeFalse());
    task.getTaskHealth().subscribe((v) => expect(v).toBeNull());
    task.listSubtasks(10).subscribe((v) => expect(v).toEqual([]));
    httpMock.match(() => true).forEach((r) => r.flush({}, { status: 500, statusText: 'err' }));
  });
});
