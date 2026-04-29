package com.esprit.ticket.service;

import com.esprit.ticket.config.GlobalExceptionHandler;
import com.esprit.ticket.config.RestClientConfig;
import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.ticket.MonthlyTicketCount;
import com.esprit.ticket.dto.ticket.TicketStatsResponse;
import com.esprit.ticket.entity.Ticket;
import com.esprit.ticket.exception.EntityNotFoundException;
import com.esprit.ticket.scheduler.TicketAutoCloseScheduler;
import com.esprit.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketCoverageBoostTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private TicketRepository ticketRepository;
    @InjectMocks
    private TicketPdfExportService pdfExportService;

    @Test
    void ticketPdfExportServiceBuildsNonEmptyPdf() {
        when(ticketService.getStats()).thenReturn(new TicketStatsResponse(10, 4, 6, 12.5));
        when(ticketService.getMonthlyStats()).thenReturn(List.of(new MonthlyTicketCount(2026, 4, 5)));
        when(ticketRepository.countGroupedByPriority()).thenReturn(java.util.Collections.singletonList(new Object[]{TicketPriority.HIGH, 3L}));
        when(ticketRepository.countByReopenCountGreaterThan(0)).thenReturn(2L);
        when(ticketRepository.findByStatusOrderByLastActivityAtDesc(eq(TicketStatus.OPEN), any())).thenReturn(List.of(
                Ticket.builder().id(1L).userId(7L).subject("Need support on invoice issue")
                        .status(TicketStatus.OPEN).priority(TicketPriority.HIGH).lastActivityAt(LocalDateTime.now()).build()
        ));

        byte[] pdf = pdfExportService.buildMonthlyReportPdf();

        assertThat(pdf).isNotEmpty();
        assertThat(pdf).hasSizeGreaterThan(200);
    }

    @Test
    void globalExceptionHandlerMapsErrors() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var nf = handler.handleEntityNotFound(new EntityNotFoundException("x"));
        assertThat(nf.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "field", "required"));
        MethodArgumentNotValidException validation = new MethodArgumentNotValidException(org.mockito.Mockito.mock(MethodParameter.class), binding);
        assertThat(handler.handleValidation(validation).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleIllegalArgument(new IllegalArgumentException("oops")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleAny(new RuntimeException("boom")).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void configAndSchedulerSmoke() {
        RestClientConfig config = new RestClientConfig();
        assertThat(config.restTemplate()).isNotNull();

        TicketAutoCloseScheduler scheduler = new TicketAutoCloseScheduler(ticketService);
        when(ticketService.autoCloseInactiveOpenTickets()).thenReturn(1);
        scheduler.autoCloseInactiveTickets();
        verify(ticketService).autoCloseInactiveOpenTickets();
    }
}
