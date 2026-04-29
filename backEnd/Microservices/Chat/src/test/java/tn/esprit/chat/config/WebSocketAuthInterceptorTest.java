package tn.esprit.chat.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketAuthInterceptorTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor();

    @Test
    void preSendSetsUserWhenConnectHeaderHasValidUserId() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader(USER_ID_HEADER, "15");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo("15");
    }

    @Test
    void preSendIgnoresInvalidNumericHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader(USER_ID_HEADER, "abc");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertThat(resultAccessor.getUser()).isNull();
    }

    @Test
    void preSendDoesNothingForNonConnectFrames() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.addNativeHeader(USER_ID_HEADER, "99");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertThat(resultAccessor.getUser()).isNull();
    }
}
