package com.esprit.ticket.controller;

import com.esprit.ticket.dto.reply.CreateReplyRequest;
import com.esprit.ticket.dto.reply.ReplyResponse;
import com.esprit.ticket.dto.reply.UpdateReplyRequest;
import com.esprit.ticket.domain.ReplySender;
import com.esprit.ticket.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyControllerTest {

    @Mock
    private ReplyService replyService;

    @InjectMocks
    private ReplyController controller;

    @Test
    void createGetUpdateDelete_delegateToReplyService() {
        CreateReplyRequest createReq = new CreateReplyRequest(7L, "hello");
        UpdateReplyRequest updateReq = new UpdateReplyRequest("edited");
        ReplyResponse response = new ReplyResponse(1L, 7L, "hello", ReplySender.USER, 100L, null, false, false);

        when(replyService.addReply(createReq)).thenReturn(response);
        when(replyService.getByTicketId(7L)).thenReturn(List.of(response));
        when(replyService.updateReply(1L, updateReq))
                .thenReturn(new ReplyResponse(1L, 7L, "edited", ReplySender.ADMIN, 200L, null, true, true));

        assertThat(controller.create(createReq).id()).isEqualTo(1L);
        assertThat(controller.getByTicket(7L)).hasSize(1);
        assertThat(controller.update(1L, updateReq).message()).isEqualTo("edited");
        controller.delete(1L);

        verify(replyService).deleteReply(1L);
    }
}
