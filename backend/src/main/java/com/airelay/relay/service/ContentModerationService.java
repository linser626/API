package com.airelay.relay.service;

import com.airelay.relay.dto.ModerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.moderation")
public class ContentModerationService {

    private boolean enabled = false;
    private List<String> blockedWords = new ArrayList<>();
    private boolean checkPromptInjection = true;
    private List<String> promptInjectionPatterns = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getBlockedWords() {
        return blockedWords;
    }

    public void setBlockedWords(List<String> blockedWords) {
        this.blockedWords = blockedWords;
    }

    public boolean isCheckPromptInjection() {
        return checkPromptInjection;
    }

    public void setCheckPromptInjection(boolean checkPromptInjection) {
        this.checkPromptInjection = checkPromptInjection;
    }

    public List<String> getPromptInjectionPatterns() {
        return promptInjectionPatterns;
    }

    public void setPromptInjectionPatterns(List<String> promptInjectionPatterns) {
        this.promptInjectionPatterns = promptInjectionPatterns;
    }

    public ModerationResult checkContent(String text) {
        if (!enabled) {
            return ModerationResult.builder().passed(true).build();
        }

        if (text == null || text.trim().isEmpty()) {
            return ModerationResult.builder().passed(true).build();
        }

        List<String> flagged = new ArrayList<>();
        String lowerText = text.toLowerCase();

        for (String word : blockedWords) {
            if (word != null && !word.trim().isEmpty() && lowerText.contains(word.toLowerCase())) {
                flagged.add(word);
            }
        }

        if (checkPromptInjection) {
            for (String pattern : promptInjectionPatterns) {
                if (pattern != null && !pattern.trim().isEmpty() && lowerText.contains(pattern.toLowerCase())) {
                    flagged.add(pattern);
                }
            }
        }

        if (!flagged.isEmpty()) {
            log.warn("内容审核拦截: flaggedWords={}", flagged);
            return ModerationResult.builder()
                    .passed(false)
                    .reason("内容包含违规词汇: " + String.join(", ", flagged))
                    .flaggedWords(flagged)
                    .build();
        }

        return ModerationResult.builder().passed(true).build();
    }
}
