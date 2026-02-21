package com.wedding.backend.service;

import com.wedding.backend.domain.LineUser;
import com.wedding.backend.domain.Message;
import com.wedding.backend.dto.MessageResponse;
import com.wedding.backend.repository.LineUserRepository;
import com.wedding.backend.repository.MessageRepository;
import com.wedding.backend.websocket.WebSocketPusher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final LineUserRepository lineUserRepository;
    private final WebSocketPusher webSocketPusher;

    @Transactional(readOnly = true)
    public List<MessageResponse> getAllMessages() {
        List<Message> messages = messageRepository.findAllByOrderByCreatedAtDesc();

        Set<String> userIds = messages.stream()
                .map(Message::getLineUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> pictureUrlMap = lineUserRepository.findAllById(userIds).stream()
                .filter(u -> u.getPictureUrl() != null)
                .collect(Collectors.toMap(LineUser::getLineUserId, LineUser::getPictureUrl));

        return messages.stream()
                .map(m -> MessageResponse.from(m, pictureUrlMap.get(m.getLineUserId())))
                .toList();
    }

    @Transactional
    public MessageResponse createMessage(String lineUserId, String displayName, String pictureUrl, String content) {
        Message message = Message.builder()
                .lineUserId(lineUserId)
                .name(displayName)
                .content(content)
                .build();

        Message saved = messageRepository.save(message);
        log.info("Saved new message from user: {}, content: {}", lineUserId, content);

        MessageResponse response = MessageResponse.from(saved, pictureUrl);
        webSocketPusher.pushNewMessage(response);

        return response;
    }
}
