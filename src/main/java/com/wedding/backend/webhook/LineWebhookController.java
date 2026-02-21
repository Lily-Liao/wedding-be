package com.wedding.backend.webhook;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.*;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.PostbackEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;
import com.wedding.backend.domain.LineUser;
import com.wedding.backend.domain.LineUserState;
import com.wedding.backend.domain.VotingSession;
import com.wedding.backend.exception.BusinessException;
import com.wedding.backend.service.HighlightService;
import com.wedding.backend.service.LineUserService;
import com.wedding.backend.service.LineUserStateService;
import com.wedding.backend.service.MessageService;
import com.wedding.backend.service.VoteService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@LineMessageHandler
@RequiredArgsConstructor
@Slf4j
public class LineWebhookController {

    // ── 觸發關鍵字 ────────────────────────────────────────────────────────────
    private static final String TRIGGER_WALL          = "留言應援牆";
    private static final String TRIGGER_WALL_EN       = "Message Wall";
    private static final String TRIGGER_VOTE          = "限時票選";
    private static final String TRIGGER_VOTE_EN       = "Live Poll";
    private static final String TRIGGER_INFO          = "演出資訊";
    private static final String TRIGGER_INFO_EN       = "Show Info";
    private static final String TRIGGER_HIGHLIGHTS    = "精選片段";
    private static final String TRIGGER_HIGHLIGHTS_EN = "Highlights";

    private final MessagingApiClient   messagingApiClient;
    private final MessageService       messageService;
    private final VoteService          voteService;
    private final LineUserStateService lineUserStateService;
    private final LineUserService      lineUserService;
    private final HighlightService     highlightService;
    private final MessageSource        messageSource;

    // ── Handler registry ──────────────────────────────────────────────────────

    @FunctionalInterface
    interface KeywordHandler {
        void handle(String replyToken, String userId, Locale locale);
    }

    private record HandlerEntry(KeywordHandler handler, Locale locale) {}

    /**
     * Keyword → (handler, locale) mapping.
     * 新增關鍵字只需在 initKeywordHandlers() 加 register()，不需要修改 dispatch 邏輯。
     */
    private final Map<String, HandlerEntry> keywordHandlers = new HashMap<>();

    @PostConstruct
    void initKeywordHandlers() {
        register(TRIGGER_WALL,          TRIGGER_WALL_EN,          this::handleWallTrigger);
        register(TRIGGER_VOTE,          TRIGGER_VOTE_EN,          this::handleVoteTrigger);
        register(TRIGGER_INFO,          TRIGGER_INFO_EN,          this::handleInfoTrigger);
        register(TRIGGER_HIGHLIGHTS,    TRIGGER_HIGHLIGHTS_EN,    this::handleHighlightsTrigger);
    }

    private void register(String zhKeyword, String enKeyword, KeywordHandler handler) {
        keywordHandlers.put(zhKeyword, new HandlerEntry(handler, Locale.TAIWAN));
        keywordHandlers.put(enKeyword, new HandlerEntry(handler, Locale.ENGLISH));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main dispatcher
    // ─────────────────────────────────────────────────────────────────────────

    @EventMapping
    public void handleTextMessage(MessageEvent event) {
        if (!(event.message() instanceof TextMessageContent textContent)) {
            return;
        }

        String userId     = event.source().userId();
        String replyToken = event.replyToken();
        String text       = textContent.text().trim();

        log.info("Received message from user: {}, text: {}", userId, text);

        try {
            LineUser lineUser = lineUserService.getOrFetch(userId);
            LineUserState userState = lineUserStateService.getState(userId);
            LineUserState.State state = userState.getCurrentState();
            Locale locale = localeFromStateData(userState);

            // 1. 優先處理 awaiting 狀態（使用者正在進行互動流程）
            if (state == LineUserState.State.AWAITING_MESSAGE) {
                handleAwaitingMessage(replyToken, userId, text, locale, lineUser);
                return;
            }
            if (state == LineUserState.State.AWAITING_VOTE) {
                handleAwaitingVote(replyToken, userId, text, locale, lineUser);
                return;
            }

            // 2. Map dispatch — 新增關鍵字只改 initKeywordHandlers()
            var entry = keywordHandlers.get(text);
            if (entry != null) {
                entry.handler().handle(replyToken, userId, entry.locale());
            } else {
                log.debug("Unrecognized message in IDLE state from user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error handling message event for user: {}", userId, e);
            reply(replyToken, List.of(new TextMessage(msg("line.error.general", Locale.TAIWAN))));
        }
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        String userId     = event.source().userId();
        String replyToken = event.replyToken();
        String data       = event.postback().data();

        log.info("Received postback from user: {}, data: {}", userId, data);

        LineUser lineUser = lineUserService.getOrFetch(userId);
        LineUserState userState = lineUserStateService.getState(userId);
        Locale locale = localeFromStateData(userState);

        if (!voteService.isValidOption(data)) {
            log.debug("Unrecognized postback data from user: {}", userId);
            return;
        }

        try {
            voteService.castVote(userId, lineUser.getDisplayName(), data);
            lineUserStateService.resetToIdle(userId);
            reply(replyToken, List.of(new TextMessage(msg("line.vote.success", locale, data))));
        } catch (BusinessException e) {
            lineUserStateService.resetToIdle(userId);
            reply(replyToken, List.of(new TextMessage(e.getMessage())));
        }
    }

    @EventMapping
    public void handleDefaultEvent(Event event) {
        log.debug("Received non-text event: {}", event.getClass().getSimpleName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Keyword handlers（每個關鍵字對應一個方法）
    // ─────────────────────────────────────────────────────────────────────────

    private void handleWallTrigger(String replyToken, String userId, Locale locale) {
        lineUserStateService.setState(userId, LineUserState.State.AWAITING_MESSAGE,
                stateDataWithLocale(locale));
        QuickReply qr = quickReply(
                msg("line.wall.quick_reply.cancel", locale),
                msg("line.wall.quick_reply.groom",  locale),
                msg("line.wall.quick_reply.bride",  locale),
                msg("line.wall.quick_reply.congratulations", locale),
                msg("line.wall.quick_reply.omg", locale)
        );
        reply(replyToken, List.of(new TextMessage(qr, null, msg("line.wall.enter", locale), null, null)));
    }

    private void handleVoteTrigger(String replyToken, String userId, Locale locale) {
        VotingSession session = voteService.getCurrentSession();
        switch (session.getStatus()) {
            case WAITING -> reply(replyToken, List.of(new TextMessage(msg("line.vote.waiting", locale))));
            case START -> {
                if (voteService.hasUserVoted(userId)) {
                    reply(replyToken, List.of(new TextMessage(msg("line.vote.already_voted", locale))));
                    return;
                }
                lineUserStateService.setState(userId, LineUserState.State.AWAITING_VOTE,
                        stateDataWithLocale(locale));
                reply(replyToken, List.of(voteService.replyVotingStartMsgWithLocale(locale)));
            }
            case CLOSED -> reply(replyToken, List.of(new TextMessage(msg("line.vote.closed", locale))));
        }
    }

    private void handleInfoTrigger(String replyToken, String userId, Locale locale) {
        reply(replyToken, List.of(
                new LocationMessage(
                        "格來天漾大飯店",
                        "臺北市萬華區艋舺大道101號",
                        25.033395573107516,
                        121.50137110351639
                )
        ));
    }

    private void handleHighlightsTrigger(String replyToken, String userId, Locale locale) {
        List<String> featuredPhotos = highlightService.getFeaturedPhotoFromCloudAlbum();
        FlexCarousel carousel = highlightService.buildCarouselImage(featuredPhotos);
        FlexMessage carouselMessage = new FlexMessage("更多精彩片段在幕後等你揭開 (ﾉˊᗜˋ)ﾉ♪", carousel);

        reply(replyToken, List.of(carouselMessage));
    }



    // ─────────────────────────────────────────────────────────────────────────
    // Awaiting-state handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleAwaitingMessage(String replyToken, String userId, String text, Locale locale, LineUser lineUser) {
        if (isCancelKeyword(text)) {
            lineUserStateService.resetToIdle(userId);
            reply(replyToken, List.of(new TextMessage(msg("line.wall.cancel.response", locale))));
            return;
        }
        messageService.createMessage(userId, lineUser.getDisplayName(), lineUser.getPictureUrl(), text);
        lineUserStateService.resetToIdle(userId);
        reply(replyToken, List.of(new TextMessage(msg("line.wall.message.sent", locale))));
    }

    private void handleAwaitingVote(String replyToken, String userId, String text, Locale locale, LineUser lineUser) {
        String option = text.trim().toUpperCase();
        VotingSession session = voteService.getCurrentSession();
        VotingSession.Status currentStatus = session.getStatus();

        if (currentStatus == VotingSession.Status.CLOSED) {
            lineUserStateService.resetToIdle(userId);
            reply(replyToken, List.of(new TextMessage(msg("line.vote.closed", locale))));
            return;
        }

        if (!voteService.isValidOption(option)) {
            QuickReply qr = new QuickReply(List.of(
                    new QuickReplyItem(new MessageAction("A 藍色 Blue",   "A")),
                    new QuickReplyItem(new MessageAction("B 灰色 Gray",   "B")),
                    new QuickReplyItem(new MessageAction("C 棕色 Brown",  "C")),
                    new QuickReplyItem(new MessageAction("D 粉色 Pink",   "D"))
            ));
            reply(replyToken, List.of(
                    new TextMessage(qr, null, msg("line.vote.invalid_option", locale), null, null)
            ));
            return;
        }
        try {
            voteService.castVote(userId, lineUser.getDisplayName(), option);
            lineUserStateService.resetToIdle(userId);
            reply(replyToken, List.of(new TextMessage(msg("line.vote.success", locale, option))));
        } catch (BusinessException e) {
            lineUserStateService.resetToIdle(userId);
            reply(replyToken, List.of(new TextMessage(e.getMessage())));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private String msg(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    private static Map<String, Object> stateDataWithLocale(Locale locale) {
        return Map.of("locale", locale.toLanguageTag());
    }

    private static Locale localeFromStateData(LineUserState userState) {
        Map<String, Object> data = userState.getStateData();
        if (data != null && data.get("locale") instanceof String tag) {
            return Locale.forLanguageTag(tag);
        }
        return Locale.TAIWAN;
    }

    /** Accepts both Chinese 取消 and English Cancel quick-reply values. */
    private static boolean isCancelKeyword(String text) {
        return "取消".equals(text) || "Cancel".equals(text);
    }

    private static QuickReply quickReply(String... labels) {
        List<QuickReplyItem> items = Arrays.stream(labels)
                .map(label -> new QuickReplyItem(new MessageAction(label, label)))
                .toList();
        return new QuickReply(items);
    }

    private void reply(String replyToken, List<Message> messages) {
        try {
            messagingApiClient.replyMessage(
                    new ReplyMessageRequest(replyToken, messages, false)
            );
            log.debug("Replied to token: {}", replyToken);
        } catch (Exception e) {
            log.error("Failed to send reply for token: {}", replyToken, e);
        }
    }
}
