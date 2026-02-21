package com.wedding.backend.websocket;

import com.wedding.backend.dto.MessageResponse;
import com.wedding.backend.dto.VoteOptionResponse;
import com.wedding.backend.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPusher {

    private static final String TOPIC_MESSAGES = "/topic/messages";
    private static final String TOPIC_VOTES = "/topic/votes";

    private final SimpMessagingTemplate messagingTemplate;

    public void pushNewMessage(MessageResponse messageResponse) {
        WebSocketMessage<MessageResponse> wsMessage = WebSocketMessage.of("message:new", messageResponse);
        log.debug("Pushing new message to WebSocket: {}", messageResponse.getId());
        messagingTemplate.convertAndSend(TOPIC_MESSAGES, wsMessage);
    }

    public void pushVoteUpdate(VoteOptionResponse voteOptionResponse) {
        WebSocketMessage<VoteOptionResponse> wsMessage = WebSocketMessage.of("vote:update", voteOptionResponse);
        log.debug("Pushing vote update to WebSocket");
        messagingTemplate.convertAndSend(TOPIC_VOTES, wsMessage);
    }
}
