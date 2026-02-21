package com.wedding.backend.webhook;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.Source;
import com.linecorp.bot.webhook.model.TextMessageContent;
import com.wedding.backend.domain.LineUserState;
import com.wedding.backend.domain.VotingSession;
import com.wedding.backend.service.HighlightService;
import com.wedding.backend.service.LineUserStateService;
import com.wedding.backend.service.MessageService;
import com.wedding.backend.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LineWebhookControllerTest {

    @Mock MessagingApiClient   messagingApiClient;
    @Mock MessageService       messageService;
    @Mock VoteService          voteService;
    @Mock LineUserStateService lineUserStateService;
    @Mock HighlightService highlightService;
    @Mock MessageSource        messageSource;

    LineWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new LineWebhookController(
                messagingApiClient, messageService, voteService,
                lineUserStateService, highlightService, messageSource
        );
        controller.initKeywordHandlers();  // 模擬 @PostConstruct

        // 讓所有 msg() 呼叫回傳 key 本身，方便 assert
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 留言牆 — 中文觸發 (Locale.TAIWAN)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("輸入「留言應援牆」→ 切換到 AWAITING_MESSAGE 並回覆帶 QuickReply")
    void triggerWall_setsAwaitingMessageState() {
        givenIdleState("u1");

        controller.handleTextMessage(mockEvent("留言應援牆", "u1", "token1"));

        verify(lineUserStateService).setState(eq("u1"), eq(LineUserState.State.AWAITING_MESSAGE), any());
        verify(messagingApiClient).replyMessage(any(ReplyMessageRequest.class));
    }

    @Test
    @DisplayName("輸入「Message Wall」→ 切換到 AWAITING_MESSAGE，locale=ENGLISH")
    void triggerWallEn_setsAwaitingMessageStateWithEnglishLocale() {
        givenIdleState("u1");

        controller.handleTextMessage(mockEvent("Message Wall", "u1", "token1"));

        verify(lineUserStateService).setState(
                eq("u1"), eq(LineUserState.State.AWAITING_MESSAGE),
                eq(Map.of("locale", "en")));
    }

    @Test
    @DisplayName("AWAITING_MESSAGE + 一般文字 → 存入 DB，切回 IDLE")
    void awaitingMessage_saveMessageAndResetToIdle() {
        givenState("u1", LineUserState.State.AWAITING_MESSAGE);

        controller.handleTextMessage(mockEvent("新娘超美", "u1", "token2"));

        verify(messageService).createMessage("u1", "u1", "新娘超美");
        verify(lineUserStateService).resetToIdle("u1");
        verify(messagingApiClient).replyMessage(any());
    }

    @Test
    @DisplayName("AWAITING_MESSAGE + 取消 → 不存 DB，切回 IDLE")
    void awaitingMessage_cancelZhDoesNotSave() {
        givenState("u1", LineUserState.State.AWAITING_MESSAGE);

        controller.handleTextMessage(mockEvent("取消", "u1", "token3"));

        verify(messageService, never()).createMessage(any(), any(), any());
        verify(lineUserStateService).resetToIdle("u1");
    }

    @Test
    @DisplayName("AWAITING_MESSAGE + Cancel (English) → 不存 DB，切回 IDLE")
    void awaitingMessage_cancelEnDoesNotSave() {
        givenStateWithLocale("u1", LineUserState.State.AWAITING_MESSAGE, Locale.ENGLISH);

        controller.handleTextMessage(mockEvent("Cancel", "u1", "token3en"));

        verify(messageService, never()).createMessage(any(), any(), any());
        verify(lineUserStateService).resetToIdle("u1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 互動投票
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("投票 WAITING 狀態 → 回覆尚未開始")
    void triggerVote_waiting() {
        givenIdleState("u2");
        givenVotingStatus(VotingSession.Status.WAITING);

        controller.handleTextMessage(mockEvent("限時投票", "u2", "token4"));

        ArgumentCaptor<ReplyMessageRequest> captor = ArgumentCaptor.forClass(ReplyMessageRequest.class);
        verify(messagingApiClient).replyMessage(captor.capture());
        assertThat(captor.getValue().messages()).hasSize(1);
    }

    @Test
    @DisplayName("投票 START 狀態 + 已投票 → 回覆已投過")
    void triggerVote_alreadyVoted() {
        givenIdleState("u3");
        givenVotingStatus(VotingSession.Status.START);
        when(voteService.hasUserVoted("u3")).thenReturn(true);

        controller.handleTextMessage(mockEvent("限時投票", "u3", "token5"));

        verify(lineUserStateService, never()).setState(any(), any(), any());
    }

    @Test
    @DisplayName("AWAITING_VOTE + 有效選項 A → 計票，切回 IDLE")
    void awaitingVote_validOption() {
        givenState("u4", LineUserState.State.AWAITING_VOTE);
        when(voteService.isValidOption("A")).thenReturn(true);

        controller.handleTextMessage(mockEvent("A", "u4", "token6"));

        verify(voteService).castVote("u4", "u4", "A");
        verify(lineUserStateService).resetToIdle("u4");
    }

    @Test
    @DisplayName("AWAITING_VOTE + 無效選項 → 不計票，再次提示")
    void awaitingVote_invalidOption() {
        givenState("u5", LineUserState.State.AWAITING_VOTE);
        when(voteService.isValidOption("Z")).thenReturn(false);

        controller.handleTextMessage(mockEvent("Z", "u5", "token7"));

        verify(voteService, never()).castVote(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 席位查詢
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("輸入「席位查詢」→ 切換到 AWAITING_SEAT_QUERY，locale=TAIWAN")
    void triggerSeat_setsAwaitingSeatState() {
        givenIdleState("u6");

        controller.handleTextMessage(mockEvent("席位查詢", "u6", "token8"));

        verify(lineUserStateService).setState(
                eq("u6"), eq(LineUserState.State.AWAITING_SEAT_QUERY),
                eq(Map.of("locale", "zh-TW")));
    }

    @Test
    @DisplayName("輸入「Reserved Spot」→ 切換到 AWAITING_SEAT_QUERY，locale=ENGLISH")
    void triggerSeatEn_setsAwaitingSeatStateWithEnglishLocale() {
        givenIdleState("u6");

        controller.handleTextMessage(mockEvent("Reserved Spot", "u6", "token8en"));

        verify(lineUserStateService).setState(
                eq("u6"), eq(LineUserState.State.AWAITING_SEAT_QUERY),
                eq(Map.of("locale", "en")));
    }

    @Test
    @DisplayName("AWAITING_SEAT_QUERY + 取消 → 切回 IDLE，不查詢")
    void awaitingSeat_cancelResetsState() {
        givenState("u7", LineUserState.State.AWAITING_SEAT_QUERY);

        controller.handleTextMessage(mockEvent("取消", "u7", "token9"));

        verify(lineUserStateService).resetToIdle("u7");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 一次性關鍵字
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("輸入「婚禮資訊」→ 回覆一則文字訊息 (zh)")
    void triggerInfo_repliesWithText() {
        givenIdleState("u8");

        controller.handleTextMessage(mockEvent("婚禮資訊", "u8", "token10"));

        verify(messagingApiClient).replyMessage(any());
        verify(lineUserStateService, never()).setState(any(), any(), any());
    }

    @Test
    @DisplayName("輸入「Show Info」→ 回覆一則文字訊息 (en)")
    void triggerInfoEn_repliesWithText() {
        givenIdleState("u8");

        controller.handleTextMessage(mockEvent("Show Info", "u8", "token10en"));

        ArgumentCaptor<ReplyMessageRequest> captor = ArgumentCaptor.forClass(ReplyMessageRequest.class);
        verify(messagingApiClient).replyMessage(captor.capture());
        // msg() stub returns the key itself; assert it was called with Locale.ENGLISH
        verify(messageSource).getMessage(eq("line.info.response"), any(), eq(Locale.ENGLISH));
    }

    @Test
    @DisplayName("輸入「精選片段」→ 回覆一則文字訊息")
    void triggerHighlights_repliesWithText() {
        givenIdleState("u9");

        controller.handleTextMessage(mockEvent("精選片段", "u9", "token11"));

        verify(messagingApiClient).replyMessage(any());
    }

    @Test
    @DisplayName("輸入「Highlights」→ 回覆一則文字訊息 (en)")
    void triggerHighlightsEn_repliesWithText() {
        givenIdleState("u9");

        controller.handleTextMessage(mockEvent("Highlights", "u9", "token11en"));

        verify(messageSource).getMessage(eq("line.highlights.more"), any(), eq(Locale.ENGLISH));
    }

    @Test
    @DisplayName("未知關鍵字 → 不回覆，不改變狀態")
    void unknownKeyword_doesNothing() {
        givenIdleState("u10");

        controller.handleTextMessage(mockEvent("隨機亂打", "u10", "token12"));

        verify(messagingApiClient, never()).replyMessage(any());
        verify(lineUserStateService, never()).setState(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void givenIdleState(String userId) {
        givenState(userId, LineUserState.State.IDLE);
    }

    private void givenState(String userId, LineUserState.State state) {
        LineUserState userState = LineUserState.builder()
                .lineUserId(userId)
                .currentState(state)
                .build();
        when(lineUserStateService.getState(userId)).thenReturn(userState);
    }

    private void givenStateWithLocale(String userId, LineUserState.State state, Locale locale) {
        LineUserState userState = LineUserState.builder()
                .lineUserId(userId)
                .currentState(state)
                .stateData(Map.of("locale", locale.toLanguageTag()))
                .build();
        when(lineUserStateService.getState(userId)).thenReturn(userState);
    }

    private void givenVotingStatus(VotingSession.Status status) {
        VotingSession session = VotingSession.builder().status(status).build();
        when(voteService.getCurrentSession()).thenReturn(session);
    }

    private static MessageEvent mockEvent(String text, String userId, String replyToken) {
        MessageEvent event         = mock(MessageEvent.class);
        Source source              = mock(Source.class);
        TextMessageContent content = mock(TextMessageContent.class);

        when(event.source()).thenReturn(source);
        when(source.userId()).thenReturn(userId);
        when(event.replyToken()).thenReturn(replyToken);
        when(event.message()).thenReturn(content);
        when(content.text()).thenReturn(text);

        return event;
    }
}
