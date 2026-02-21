package com.wedding.backend.service;

import com.wedding.backend.domain.LineUserState;
import com.wedding.backend.repository.LineUserStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LineUserStateService {

    private final LineUserStateRepository lineUserStateRepository;

    @Transactional(readOnly = true)
    public LineUserState getState(String lineUserId) {
        return lineUserStateRepository.findById(lineUserId)
                .orElse(LineUserState.builder()
                        .lineUserId(lineUserId)
                        .currentState(LineUserState.State.IDLE)
                        .build());
    }

    /** Transitions state and stores arbitrary context (e.g. locale preference). */
    @Transactional
    public LineUserState setState(String lineUserId, LineUserState.State state,
                                  Map<String, Object> stateData) {
        LineUserState userState = lineUserStateRepository.findById(lineUserId)
                .orElse(LineUserState.builder()
                        .lineUserId(lineUserId)
                        .build());
        userState.setCurrentState(state);
        userState.setStateData(stateData);
        return lineUserStateRepository.save(userState);
    }

    /** Resets to IDLE and clears stateData. */
    @Transactional
    public LineUserState resetToIdle(String lineUserId) {
        return setState(lineUserId, LineUserState.State.IDLE, null);
    }
}
