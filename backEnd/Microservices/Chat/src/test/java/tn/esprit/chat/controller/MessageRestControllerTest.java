package tn.esprit.chat.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import tn.esprit.chat.dto.ChatMessageDTO;
import tn.esprit.chat.dto.ConversationSummary;
import tn.esprit.chat.dto.TranslationRequest;
import tn.esprit.chat.dto.TranslationResponse;
import tn.esprit.chat.service.IChatService;
import tn.esprit.chat.service.TranslationService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRestControllerTest {

    @Mock
    private IChatService chatService;

    @Mock
    private TranslationService translationService;

    @InjectMocks
    private MessageRestController controller;

    @Test
    void translate_usesAutoWhenSourceLangBlank() {
        TranslationRequest request = new TranslationRequest();
        request.setText("hello");
        request.setTargetLang("fr");
        request.setSourceLang(" ");
        when(translationService.translate("hello", "fr", "auto")).thenReturn("bonjour");

        ResponseEntity<TranslationResponse> response = controller.translate(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTranslatedText()).isEqualTo("bonjour");
    }

    @Test
    void conversationAndUnreadEndpoints_delegateToService() {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(10L);
        when(chatService.getConversation(1L, 2L, 0, 20)).thenReturn(new PageImpl<>(List.of(dto)));
        when(chatService.getUnreadMessages(2L)).thenReturn(List.of(dto));
        when(chatService.getUnreadCount(2L)).thenReturn(Map.of("total", 1L));
        when(chatService.markAsSeen(10L)).thenReturn(dto);
        when(chatService.getConversations(2L)).thenReturn(List.of(new ConversationSummary()));

        assertThat(controller.getConversation(1L, 2L, 0, 20).getBody().getTotalElements()).isEqualTo(1);
        assertThat(controller.getUnreadMessages(2L).getBody()).hasSize(1);
        assertThat(controller.getUnreadCount(2L).getBody()).containsEntry("total", 1L);
        assertThat(controller.markAsSeen(10L).getBody().getId()).isEqualTo(10L);
        assertThat(controller.getConversations(2L).getBody()).hasSize(1);
        verify(chatService).getConversations(2L);
    }
}
