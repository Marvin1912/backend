package com.marvin.grocery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.entity.ArticleEntity;
import com.marvin.grocery.repository.ArticleRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService Tests")
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    @DisplayName("Should return the existing article when one matches the normalized name")
    void findOrCreate_ExistingNormalizedName_ReturnsExistingArticle() {
        final ArticleEntity existing = new ArticleEntity();
        existing.setId(1L);
        existing.setName("Milch");
        existing.setNormalizedName("milch");
        when(articleRepository.findByNormalizedName("milch")).thenReturn(Optional.of(existing));

        final ArticleEntity result = articleService.findOrCreate(" Milch ");

        assertSame(existing, result);
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create and persist a new article when no match exists for the normalized name")
    void findOrCreate_NoMatch_CreatesAndPersistsNewArticle() {
        when(articleRepository.findByNormalizedName("kaffee")).thenReturn(Optional.empty());
        when(articleRepository.save(any(ArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final ArticleEntity result = articleService.findOrCreate("Kaffee");

        assertEquals("Kaffee", result.getName());
        assertEquals("kaffee", result.getNormalizedName());
        verify(articleRepository).save(any(ArticleEntity.class));
    }

    @Test
    @DisplayName("Should normalize by trimming whitespace and lower-casing before looking up an existing article")
    void findOrCreate_NormalizesNameBeforeLookup() {
        when(articleRepository.findByNormalizedName(eq("apfel"))).thenReturn(Optional.empty());
        when(articleRepository.save(any(ArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        articleService.findOrCreate("  APFEL  ");

        verify(articleRepository).findByNormalizedName("apfel");
    }
}
