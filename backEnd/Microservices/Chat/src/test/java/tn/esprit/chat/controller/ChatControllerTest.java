package tn.esprit.chat.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tn.esprit.chat.dto.ChatMessageDTO;
import tn.esprit.chat.dto.SendMessageRequest;
import tn.esprit.chat.dto.TypingEvent;
import tn.esprit.chat.service.IChatService;

import java.security.Principal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private IChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatController chatController;

    @Test
    void handleMessage_setsSenderAndRoutesToBothUsers() {
        Principal principal = () -> "7";
        SendMessageRequest request = new SendMessageRequest(null, 9L, "hello");
        ChatMessageDTO saved = new ChatMessageDTO();
        saved.setId(100L);
        saved.setSenderId(7L);
        saved.setReceiverId(9L);
        when(chatService.sendMessage(request)).thenReturn(saved);

        chatController.handleMessage(request, principal);

        assertThat(request.getSenderId()).isEqualTo(7L);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("/queue/messages"),
                org.mockito.ArgumentMatchers.eq(saved));
    }

    @Test
    void handleTyping_andSeen_behaveCorrectly() {
        Principal principal = () -> "3";
        TypingEvent event = new TypingEvent();
        event.setReceiverId(4L);
        chatController.handleTyping(event, principal);
        assertThat(event.getSenderId()).isEqualTo(3L);

        chatController.handleSeen(Map.of(), principal);

        ChatMessageDTO updated = new ChatMessageDTO();
        updated.setSenderId(8L);
        when(chatService.markAsSeen(55L)).thenReturn(updated);
        chatController.handleSeen(Map.of("messageId", 55L), principal);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(userCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("/queue/seen"),
                org.mockito.ArgumentMatchers.eq(updated));
        assertThat(userCaptor.getValue()).isEqualTo("8");
    }
}
