package com.wedding.backend.service;

import com.linecorp.bot.messaging.model.*;
import com.wedding.backend.domain.Vote;
import com.wedding.backend.domain.VotingSession;
import com.wedding.backend.dto.VoteOptionResponse;
import com.wedding.backend.dto.VoteOptionResponse.VoteOption;
import com.wedding.backend.dto.VotingOptionItem;
import com.wedding.backend.exception.BusinessException;
import com.wedding.backend.exception.ResourceNotFoundException;
import com.wedding.backend.repository.VoteRepository;
import com.wedding.backend.repository.VotingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private static final Set<String> VALID_OPTIONS = Set.of("A", "B", "C", "D");

    private static final List<VotingOptionItem> DEFAULT_OPTIONS = List.of(
            new VotingOptionItem("A", "海風清透藍 Sea Blue",      "#AAC6E6"),
            new VotingOptionItem("B", "星塵霧銀灰 Silver Gray",   "#A7A2A2C9"),
            new VotingOptionItem("C", "可可焦糖棕 Caramel Brown", "#BA8663"),
            new VotingOptionItem("D", "霓光櫻花粉 Blossom Pink",  "#F4BDE0")
    );

    private final VoteRepository voteRepository;
    private final VotingSessionRepository votingSessionRepository;
    private final MessageSource messageSource;

    @Transactional(readOnly = true)
    public VotingSession getCurrentSession() {
        return votingSessionRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No voting session found"));
    }

    @Transactional
    public VotingSession updateSessionStatus(VotingSession.Status newStatus) {
        VotingSession session = getCurrentSession();
        VotingSession.Status currentStatus = session.getStatus();

        if (newStatus == VotingSession.Status.START && currentStatus != VotingSession.Status.WAITING) {
            throw new BusinessException("Can only start voting from WAITING status");
        }
        if (newStatus == VotingSession.Status.CLOSED && currentStatus != VotingSession.Status.START) {
            throw new BusinessException("Can only close voting from START status");
        }

        session.setStatus(newStatus);
        if (newStatus == VotingSession.Status.START) {
            session.setStartedAt(OffsetDateTime.now());
        } else if (newStatus == VotingSession.Status.CLOSED) {
            session.setClosedAt(OffsetDateTime.now());
        }

        return votingSessionRepository.save(session);
    }

    @Transactional
    public void castVote(String lineUserId, String lineDisplayName, String optionKey) {
        if (!VALID_OPTIONS.contains(optionKey.toUpperCase())) {
            throw new BusinessException("Invalid option. Please choose A, B, C, or D.");
        }

        VotingSession session = getCurrentSession();

        if (session.getStatus() != VotingSession.Status.START) {
            throw new BusinessException("Voting is not currently active.");
        }

        if (voteRepository.existsByVotingSessionAndLineUserId(session, lineUserId)) {
            throw new BusinessException("You have already voted in this session.");
        }

        Vote vote = Vote.builder()
                .votingSession(session)
                .lineUserId(lineUserId)
                .lineDisplayName(lineDisplayName)
                .optionKey(optionKey.toUpperCase())
                .build();

        voteRepository.save(vote);
        log.info("User {} voted for option {} in session {}", lineUserId, optionKey, session.getId());
    }

    @Transactional(readOnly = true)
    public VoteOptionResponse getVoteOptions() {
        VotingSession session = getCurrentSession();
        return buildVoteOptionResponse(session);
    }

    private VoteOptionResponse buildVoteOptionResponse(VotingSession session) {
        List<Object[]> counts = voteRepository.countByOptionKeyForSession(session);
        long total = voteRepository.countByVotingSession(session);

        Map<String, Long> countMap = new HashMap<>();
        for (Object[] row : counts) {
            countMap.put((String) row[0], (Long) row[1]);
        }

        Map<String, VotingOptionItem> optionMap = getEffectiveOptions(session).stream()
                .collect(Collectors.toMap(VotingOptionItem::getKey, o -> o));

        List<VoteOption> options = VALID_OPTIONS.stream()
                .sorted()
                .map(key -> {
                    long count = countMap.getOrDefault(key, 0L);
                    double percentage = total > 0 ? (double) count / total * 100 : 0.0;
                    VotingOptionItem item = optionMap.getOrDefault(key, new VotingOptionItem(key, key, "#CCCCCC"));
                    return VoteOption.builder()
                            .key(key)
                            .label(item.getLabel())
                            .color(item.getColor())
                            .count(count)
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .toList();

        return VoteOptionResponse.builder()
                .options(options)
                .totalVotes(total)
                .correctAnswer(session.getCorrectAnswer())
                .build();
    }

    private List<VotingOptionItem> getEffectiveOptions(VotingSession session) {
        List<VotingOptionItem> opts = session.getOptions();
        return (opts != null && !opts.isEmpty()) ? opts : DEFAULT_OPTIONS;
    }

    @Transactional
    public VotingSession updateSessionOptions(List<VotingOptionItem> options) {
        VotingSession session = getCurrentSession();
        if (session.getStatus() != VotingSession.Status.WAITING) {
            throw new BusinessException("Options can only be updated when session status is WAITING");
        }
        session.setOptions(options);
        return votingSessionRepository.save(session);
    }

    public boolean isValidOption(String optionKey) {
        return VALID_OPTIONS.contains(optionKey.toUpperCase());
    }

    public boolean hasUserVoted(String lineUserId) {
        VotingSession session = getCurrentSession();
        return voteRepository.existsByVotingSessionAndLineUserId(session, lineUserId);
    }

    public FlexMessage replyVotingStartMsgWithLocale(Locale locale) {
        VotingSession session = getCurrentSession();
        List<VotingOptionItem> options = getEffectiveOptions(session);

        List<FlexComponent> contents = new ArrayList<>();
        contents.add(new FlexText.Builder()
                .text(msg("line.vote.start.prompt", locale))
                .wrap(true)
                .build());

        for (VotingOptionItem opt : options) {
            contents.add(createVoteButton(opt));
        }

        FlexBox body = new FlexBox.Builder(FlexBox.Layout.VERTICAL, contents).build();
        FlexBubble bubble = new FlexBubble.Builder().body(body).build();
        return new FlexMessage("粉絲互動", bubble);
    }

    private FlexButton createVoteButton(VotingOptionItem opt) {
        return new FlexButton.Builder(new PostbackAction(
                        opt.getKey() + " " + opt.getLabel(),
                        opt.getKey(),
                        null, null, null, null))
                .style(FlexButton.Style.PRIMARY)
                .color(opt.getColor())
                .height(FlexButton.Height.SM) // 2x2 佈局建議縮短按鈕高度
                .margin("sm")
                .build();
    }

    private String msg(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }
}
