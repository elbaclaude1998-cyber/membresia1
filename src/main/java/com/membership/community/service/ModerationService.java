package com.membership.community.service;

import com.membership.community.domain.ContentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Moderación básica: filtra contenido contra una lista de términos prohibidos.
 * Si detecta alguno, marca el contenido como FLAGGED (oculto del listado público
 * hasta revisión). La lista es configurable vía `community.moderation.banned-words`.
 */
@Service
public class ModerationService {

    private final List<String> bannedWords;

    public ModerationService(
            @Value("${community.moderation.banned-words:spam,scam,abuse,phishing}") String bannedCsv) {
        this.bannedWords = Arrays.stream(bannedCsv.split(","))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** Devuelve FLAGGED si el texto contiene algún término prohibido, VISIBLE en caso contrario. */
    public ContentStatus screen(String text) {
        if (text == null || text.isBlank()) {
            return ContentStatus.VISIBLE;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        boolean flagged = bannedWords.stream().anyMatch(lower::contains);
        return flagged ? ContentStatus.FLAGGED : ContentStatus.VISIBLE;
    }
}
