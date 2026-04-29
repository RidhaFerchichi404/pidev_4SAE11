package tn.esprit.chat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import tn.esprit.chat.dto.UserStatusEvent;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserStatusServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private UserStatusService userStatusService;

    @Test
    void handleSessionConnect_marksUserOnlineAndBroadcasts() {
        SessionConnectEvent event = new SessionConnectEvent(this, messageWithUser("42"));

        userStatusService.handleSessionConnect(event);

        assertThat(userStatusService.isOnline(42L)).isTrue();
        ArgumentCaptor<UserStatusEvent> captor = ArgumentCaptor.forClass(UserStatusEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user-status"), captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(42L);
        assertThat(captor.getValue().getStatus()).isEqualTo("ONLINE");
    }

    @Test
    void handleSessionDisconnect_marksUserOfflineAndBroadcasts() {
        userStatusService.handleSessionConnect(new SessionConnectEvent(this, messageWithUser("7")));
        SessionDisconnectEvent disconnect = new SessionDisconnectEvent(this, messageWithUser("7"), "session-7", CloseStatus.NORMAL);

        userStatusService.handleSessionDisconnect(disconnect);

        assertThat(userStatusService.isOnline(7L)).isFalse();
    }

    @Test
    void handleSessionConnect_ignoresInvalidPrincipalName() {
        userStatusService.handleSessionConnect(new SessionConnectEvent(this, messageWithUser("not-a-number")));

        assertThat(userStatusService.getOnlineUsers()).isEmpty();
    }

    private Message<byte[]> messageWithUser(String principalName) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
        accessor.setUser(new Principal() {
            @Override
            public String getName() {
                return principalName;
            }
        });
        accessor.setLeaveMutable(true);
        return org.springframework.messaging.support.MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
