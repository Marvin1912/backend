package com.marvin.vocabulary.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO representing a vocabulary deck.
 *
 * @param id the deck ID
 * @param name the deck name
 */
public record Deck(
        Integer id,
        @NotBlank String name
) {
}
