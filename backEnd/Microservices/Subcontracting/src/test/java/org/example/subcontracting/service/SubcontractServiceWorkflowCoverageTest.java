package org.example.subcontracting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.subcontracting.client.*;
import org.example.subcontracting.client.dto.OfferApplicationRemoteDto;
import org.example.subcontracting.client.dto.OfferRemoteDto;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.request.*;
import org.example.subcontracting.entity.*;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubcontractServiceWorkflowCoverageTest {

    @Mock private SubcontractRepository subcontractRepo;
    @Mock private SubcontractDeliverableRepository deliverableRepo;
    @Mock private UserFeignClient userClient;
    @Mock private ProjectFeignClient projectClient;
    @Mock private OfferApplicationFeignClient offerApplicationClient;
    @Mock private OfferFeignClient offerClient;
    @Mock private NotificationFeignClient notificationClient;
    @Mock private SubcontractAuditService auditService;
    @Mock private SubcontractEmailService subcontractEmailService;
    @Mock private SubcontractCoachingService coachingService;

    @Test
    void coversCreateUpdateWorkflowAndDeliverablePaths() {
        SubcontractService service = new SubcontractService(
                subcontractRepo, deliverableRepo, userClient, projectClient, offerApplicationClient, offerClient,
                notificationClient, auditService, subcontractEmailService, coachingService, new ObjectMapper()
        );
        Subcontract shared = sharedSubcontract();
        SubcontractDeliverable sharedDeliverable = deliverable(7L, shared, DeliverableStatus.PENDING);

        when(subcontractRepo.save(any(Subcontract.class))).thenAnswer(inv -> {
            Subcontract s = inv.getArgument(0);
            if (s.getId() == null) s.setId(50L);
            return s;
        });
        when(subcontractRepo.findById(50L)).thenReturn(Optional.of(shared));
        when(subcontractRepo.findByMainFreelancerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(shared));
        when(subcontractRepo.findBySubcontractorIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(shared));
        when(subcontractRepo.findByProjectIdOrderByCreatedAtDesc(99L)).thenReturn(List.of(shared));
        when(subcontractRepo.findByStatusOrderByCreatedAtDesc(any(SubcontractStatus.class))).thenReturn(List.of(shared));
        when(subcontractRepo.findAll()).thenReturn(List.of(shared));
        when(offerApplicationClient.listAcceptedForFreelancerOwnedOffers(1L)).thenReturn(List.of(offerApp(88L)));
        when(deliverableRepo.countBySubcontractId(anyLong())).thenReturn(1L);
        when(deliverableRepo.countBySubcontractIdAndStatus(anyLong(), eq(DeliverableStatus.APPROVED))).thenReturn(1L);
        when(deliverableRepo.countBySubcontractIdAndStatus(anyLong(), eq(DeliverableStatus.PENDING))).thenReturn(0L);
        when(deliverableRepo.countBySubcontractIdAndStatus(anyLong(), eq(DeliverableStatus.IN_PROGRESS))).thenReturn(0L);
        when(deliverableRepo.countBySubcontractIdAndStatus(anyLong(), eq(DeliverableStatus.SUBMITTED))).thenReturn(0L);
        when(deliverableRepo.findBySubcontractIdOrderByDeadlineAsc(anyLong())).thenReturn(List.of(sharedDeliverable));
        when(deliverableRepo.save(any(SubcontractDeliverable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.getUserById(anyLong())).thenReturn(user(1L, "Main", "User"), user(2L, "Sub", "User"));
        when(offerClient.getOfferById(anyLong())).thenReturn(offer("Offer A"));
        when(deliverableRepo.findById(7L)).thenReturn(Optional.of(sharedDeliverable));

        SubcontractRequest req = new SubcontractRequest();
        req.setSubcontractorId(2L);
        req.setOfferId(88L);
        req.setTitle("Subcontract mission");
        req.setScope("Implement microservice API and tests.");
        req.setCategory("DEVELOPMENT");
        req.setBudget(BigDecimal.valueOf(1000));
        req.setStartDate(LocalDate.now());
        req.setDeadline(LocalDate.now().plusDays(10));
        req.setRequiredSkills(List.of("Java", "Spring"));
        req.setMediaUrl("http://cdn/video.mp4");
        req.setMediaType("VIDEO");

        var created = service.create(1L, req);
        assertThat(created.getId()).isEqualTo(50L);
        assertThat(created.getRequiredSkills()).contains("Java");

        var fetched = service.getById(50L);
        assertThat(fetched.getMainFreelancerName()).isNotBlank();
        assertThat(service.getByMainFreelancer(1L)).hasSize(1);
        assertThat(service.getBySubcontractor(2L)).hasSize(1);
        assertThat(service.getByProject(99L)).hasSize(1);
        assertThat(service.getByStatus("DRAFT")).hasSize(1);
        assertThat(service.getAll()).hasSize(1);

        service.update(50L, req);
        shared.setStatus(SubcontractStatus.DRAFT);
        service.propose(50L);
        service.accept(50L);
        service.cancel(50L, "later");
        shared.setStatus(SubcontractStatus.ACCEPTED);
        service.startWork(50L);
        service.complete(50L);
        shared.setStatus(SubcontractStatus.COMPLETED);
        service.close(50L);
        shared.setStatus(SubcontractStatus.PROPOSED);
        service.reject(50L, "no");
        service.reopen(50L);

        DeliverableRequest dr = new DeliverableRequest();
        dr.setTitle("API docs");
        dr.setDescription("Write docs");
        dr.setDeadline(LocalDate.now().plusDays(1));
        service.addDeliverable(50L, dr);
        assertThat(service.getDeliverables(50L)).hasSize(1);
        service.updateDeliverable(7L, dr);

        DeliverableSubmitRequest submit = new DeliverableSubmitRequest();
        submit.setSubmissionUrl("https://example.com/out");
        submit.setSubmissionNote("done");
        service.submitDeliverable(7L, submit);

        DeliverableReviewRequest review = new DeliverableReviewRequest();
        review.setApproved(true);
        review.setReviewNote("ok");
        service.reviewDeliverable(7L, review);

        verify(notificationClient, atLeastOnce()).sendNotification(any());
        verify(auditService, atLeastOnce()).record(anyLong(), anyLong(), anyString(), any(), any(), anyString(), any(), any());
    }

    private static Subcontract sharedSubcontract() {
        Subcontract s = new Subcontract();
        s.setId(50L);
        s.setMainFreelancerId(1L);
        s.setSubcontractorId(2L);
        s.setProjectId(99L);
        s.setOfferId(88L);
        s.setTitle("Shared");
        s.setScope("scope text that is long enough for tests");
        s.setCategory(SubcontractCategory.DEVELOPMENT);
        s.setBudget(BigDecimal.valueOf(1000));
        s.setCurrency("TND");
        s.setStatus(SubcontractStatus.DRAFT);
        s.setStartDate(LocalDate.now());
        s.setDeadline(LocalDate.now().plusDays(10));
        return s;
    }

    private static OfferApplicationRemoteDto offerApp(Long offerId) {
        OfferApplicationRemoteDto a = new OfferApplicationRemoteDto();
        a.setOfferId(offerId);
        return a;
    }

    private static UserRemoteDto user(Long id, String first, String last) {
        UserRemoteDto u = new UserRemoteDto();
        u.setId(id);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(first.toLowerCase() + "@x.tn");
        return u;
    }

    private static OfferRemoteDto offer(String title) {
        OfferRemoteDto o = new OfferRemoteDto();
        o.setTitle(title);
        return o;
    }

    private static SubcontractDeliverable deliverable(Long id, Subcontract sc, DeliverableStatus status) {
        SubcontractDeliverable d = new SubcontractDeliverable();
        d.setId(id);
        d.setSubcontract(sc);
        d.setTitle("d");
        d.setStatus(status);
        d.setDeadline(LocalDate.now().plusDays(1));
        return d;
    }
}
