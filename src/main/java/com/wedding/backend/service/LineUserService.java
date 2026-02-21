package com.wedding.backend.service;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.UserProfileResponse;
import com.wedding.backend.domain.LineUser;
import com.wedding.backend.repository.LineUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LineUserService {

    private final LineUserRepository lineUserRepository;
    private final MessagingApiClient messagingApiClient;

    /**
     * 若 DB 已有此 userId 直接回傳；否則呼叫 LINE Profile API 取得資料後儲存再回傳。
     */
    @Transactional
    public LineUser getOrFetch(String userId) {
        return lineUserRepository.findById(userId)
                .orElseGet(() -> fetchAndSave(userId));
    }

    private LineUser fetchAndSave(String userId) {
        try {
            UserProfileResponse profile = messagingApiClient.getProfile(userId).get().body();
            LineUser user = LineUser.builder()
                    .lineUserId(userId)
                    .displayName(profile.displayName())
                    .pictureUrl(profile.pictureUrl() != null ? profile.pictureUrl().toString() : null)
                    .language(profile.language())
                    .build();
            LineUser saved = lineUserRepository.save(user);
            log.info("Saved LINE profile for user: {} ({})", profile.displayName(), userId);
            return saved;
        } catch (Exception e) {
            log.warn("Failed to fetch LINE profile for user: {}", userId, e);
            // 呼叫 API 失敗時回傳暫時物件，不儲存 DB
            return LineUser.builder()
                    .lineUserId(userId)
                    .displayName(userId)
                    .build();
        }
    }
}
