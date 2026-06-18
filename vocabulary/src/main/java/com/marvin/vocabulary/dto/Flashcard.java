package com.marvin.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO representing a vocabulary flashcard.
 *
 * @param id the flashcard ID
 * @param deckId the ID of the deck this flashcard belongs to
 * @param deck the name of the deck this flashcard belongs to
 * @param ankiId the corresponding Anki note ID
 * @param front the front side text
 * @param back the back side text
 * @param description an optional description
 * @param updated whether the flashcard was updated
 */
public record Flashcard(
        Integer id,
        @NotNull Integer deckId,
        String deck,
        String ankiId,
        @NotBlank String front,
        @NotBlank String back,
        String description,
        boolean updated
) {
}
