package com.esprit.ticket.controller;

import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.ticket.TicketPageResponse;
import com.esprit.ticket.dto.ticket.TicketResponse;
import com.esprit.ticket.service.TicketPdfExportService;
import com.esprit.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private TicketPdfExportService ticketPdfExportService;

    @InjectMocks
    private TicketController controller;

    @Test
    void exportPdfAndFetchOperations_delegateToServices() {
        when(ticketPdfExportService.buildMonthlyReportPdf()).thenReturn(new byte[]{1, 2});
        when(ticketService.getAll(null, null, null, "lastActivityAt", "desc", 0, 20))
                .thenReturn(new TicketPageResponse(List.of(), 0, 20, 0, 0L));
        when(ticketService.getById(9L)).thenReturn(ticket(9L, TicketStatus.OPEN, null));

        ResponseEntity<byte[]> pdf = controller.exportPdf();
        TicketPageResponse page = controller.getAll(null, null, null, "lastActivityAt", "desc", 0, 20);
        TicketResponse byId = controller.getById(9L);

        assertThat(pdf.getBody()).containsExactly(1, 2);
        assertThat(page.items()).isEmpty();
        assertThat(byId.id()).isEqualTo(9L);
    }

    @Test
    void mutationOperations_delegateToServiceMethods() {
        when(ticketService.reopen(1L)).thenReturn(ticket(1L, TicketStatus.OPEN, null));
        when(ticketService.close(2L)).thenReturn(ticket(2L, TicketStatus.CLOSED, null));
        when(ticketService.assignToCurrentAdmin(3L)).thenReturn(ticket(3L, null, TicketPriority.HIGH));
        when(ticketService.unassign(4L)).thenReturn(ticket(4L, null, null));

        assertThat(controller.reopen(1L).status()).isEqualTo(TicketStatus.OPEN);
        assertThat(controller.close(2L).status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(controller.assign(3L).priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(controller.unassign(4L).id()).isEqualTo(4L);

        controller.markRead(5L);
        controller.delete(6L);
        verify(ticketService).markTicketRepliesRead(5L);
        verify(ticketService).delete(6L);
    }

    private TicketResponse ticket(Long id, TicketStatus status, TicketPriority priority) {
        return new TicketResponse(
                id, 1L, "subject", status, priority,
                null, null, null, null, null, null, null, null, 0, true
        );
    }
}
