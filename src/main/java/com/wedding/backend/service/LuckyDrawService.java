package com.wedding.backend.service;

import com.wedding.backend.domain.Vote;
import com.wedding.backend.domain.VotingSession;
import com.wedding.backend.domain.Winner;
import com.wedding.backend.dto.ParticipantResponse;
import com.wedding.backend.dto.WinnerResponse;
import com.wedding.backend.exception.BusinessException;
import com.wedding.backend.exception.ResourceNotFoundException;
import com.wedding.backend.repository.VoteRepository;
import com.wedding.backend.repository.WinnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LuckyDrawService {

    private final VoteRepository voteRepository;
    private final WinnerRepository winnerRepository;
    private final VoteService voteService;
    private final Random random = new Random();

    @Transactional(readOnly = true)
    public ParticipantResponse getEligibleParticipants() {
        VotingSession session = voteService.getCurrentSession();
        List<Vote> eligible = resolveEligible(session);

        List<ParticipantResponse.Participant> participants = eligible.stream()
                .map(vote -> ParticipantResponse.Participant.builder()
                        .voteId(vote.getId())
                        .lineUserId(vote.getLineUserId())
                        .lineDisplayName(vote.getLineDisplayName())
                        .optionKey(vote.getOptionKey())
                        .build())
                .toList();

        return ParticipantResponse.builder()
                .data(participants)
                .metadata(ParticipantResponse.Metadata.builder()
                        .totalCount(participants.size())
                        .updatedAt(OffsetDateTime.now())
                        .build())
                .build();
    }

    @Transactional
    public WinnerResponse drawWinner() {
        VotingSession session = voteService.getCurrentSession();
        List<Vote> eligible = resolveEligible(session);

        if (eligible.isEmpty()) {
            throw new BusinessException("No eligible participants for the lucky draw.");
        }

        int index = random.nextInt(eligible.size());
        Vote selectedVote = eligible.get(index);

        Winner winner = Winner.builder()
                .vote(selectedVote)
                .lineUserId(selectedVote.getLineUserId())
                .lineDisplayName(selectedVote.getLineDisplayName())
                .optionKey(selectedVote.getOptionKey())
                .isActive(true)
                .build();

        Winner saved = winnerRepository.save(winner);
        log.info("Lucky draw winner: {} ({})", saved.getLineDisplayName(), saved.getLineUserId());

        return WinnerResponse.from(saved);
    }

    private List<Vote> resolveEligible(VotingSession session) {
        String correctAnswer = session.getCorrectAnswer();
        if (correctAnswer == null) {
            throw new BusinessException("Correct answer has not been set for this voting session.");
        }
        return voteRepository.findEligibleForDraw(session, correctAnswer);
    }

    @Transactional(readOnly = true)
    public List<WinnerResponse> getAllWinners() {
        return winnerRepository.findAllByOrderByDrawnAtDesc()
                .stream()
                .map(WinnerResponse::from)
                .toList();
    }

    @Transactional
    public void resetWinners() {
        winnerRepository.deleteAll();
        log.info("All winners have been cleared. Lucky draw pool reset.");
    }

    @Transactional
    public void cancelWinner(UUID winnerId) {
        Winner winner = winnerRepository.findById(winnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Winner", "id", winnerId));

        winner.setIsActive(false);
        winner.setCancelledAt(OffsetDateTime.now());
        winnerRepository.save(winner);

        log.info("Cancelled winner: {}", winnerId);
    }
}
