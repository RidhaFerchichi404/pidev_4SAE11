import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SubcontractService } from './subcontract.service';

describe('SubcontractService', () => {
  let service: SubcontractService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SubcontractService],
    });
    service = TestBed.inject(SubcontractService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('covers CRUD and workflow endpoint mappings', () => {
    service.getAll().subscribe();
    service.getById(1).subscribe();
    service.getByFreelancer(2).subscribe();
    service.getBySubcontractor(3).subscribe();
    service.getByProject(4).subscribe();
    service.getByStatus('DRAFT').subscribe();
    service.create(9, { subcontractorId: 5, title: 'x', category: 'DEVELOPMENT' }).subscribe();
    service.update(1, { subcontractorId: 5, title: 'x', category: 'DEVELOPMENT' }).subscribe();
    service.delete(1).subscribe();
    service.propose(1).subscribe();
    service.accept(1).subscribe();
    service.counterOffer(1, 7, { proposedBudget: 100, proposedDurationDays: 7 }).subscribe();
    service.aiMediate(1, 9, { note: 'help' }).subscribe();
    service.reject(1, 'reason').subscribe();
    service.startWork(1).subscribe();
    service.complete(1).subscribe();
    service.cancel(1, 'cancel').subscribe();
    service.close(1).subscribe();
    service.reopen(1).subscribe();

    const requests = httpMock.match(() => true);
    expect(requests.length).toBe(19);
    requests.forEach((r) => r.flush({}));
  });

  it('covers deliverables, messaging, analytics and AI endpoints', () => {
    service.getDeliverables(1).subscribe();
    service.addDeliverable(1, { title: 'd' }).subscribe();
    service.updateDeliverable(1, 2, { title: 'u' }).subscribe();
    service.deleteDeliverable(1, 2).subscribe();
    service.submitDeliverable(1, 2, { submissionNote: 'ok' }).subscribe();
    service.reviewDeliverable(1, 2, { approved: true }).subscribe();
    service.getMessages(1, 11).subscribe();
    service.sendMessage(1, 11, 'hello').subscribe();
    service.getScore(3).subscribe();
    service.getDashboard().subscribe();
    service.getSubcontractHistory(1).subscribe();
    service.getFreelancerHistory(1).subscribe();
    service.getFinancialAnalysis(1, 9).subscribe();
    service.getRiskCockpit({ mainFreelancerId: 9 }).subscribe();
    service.getPredictiveDashboard(9).subscribe();
    service.getMyCoachingProfile(9).subscribe();
    service.auditRiskSimulation(9, { totalRiskScore: 10, level: 'LOW', streamedNarrative: '', gauges: [], recommendations: [], alternatives: [] }).subscribe();
    service.confirmRisk(1, 9, { totalRiskScore: 10 }).subscribe();
    service.matchSubcontractor(9, ['java']).subscribe();
    const file = new File(['a'], 'audio.mp3', { type: 'audio/mpeg' });
    service.uploadPresentationMedia(9, file).subscribe();

    const requests = httpMock.match(() => true);
    expect(requests.length).toBe(20);
    requests.forEach((r) => r.flush({}));
  });
});
