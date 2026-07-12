package com.marvin.grocery.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.repository.ArticleGroupRepository;
import com.marvin.grocery.repository.ArticleRepository;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleGroupDeletionService Tests")
class ArticleGroupDeletionServiceTest {

    @Mock
    private ArticleGroupRepository articleGroupRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleGroupDeletionService articleGroupDeletionService;

    @Test
    @DisplayName("Should clear article group references and delete the group when it exists")
    void deleteAndDetach_GroupExists_ClearsReferencesThenDeletesGroup() {
        when(articleGroupRepository.existsById(1L)).thenReturn(true);

        articleGroupDeletionService.deleteAndDetach(1L);

        verify(articleRepository).clearArticleGroup(1L);
        verify(articleGroupRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException and not touch articles when group does not exist")
    void deleteAndDetach_GroupNotFound_ThrowsAndDoesNotTouchArticles() {
        when(articleGroupRepository.existsById(99L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> articleGroupDeletionService.deleteAndDetach(99L));

        verify(articleRepository, never()).clearArticleGroup(99L);
        verify(articleGroupRepository, never()).deleteById(99L);
    }
}
