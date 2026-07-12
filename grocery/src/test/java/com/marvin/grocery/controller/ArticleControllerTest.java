package com.marvin.grocery.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marvin.grocery.dto.ArticleDTO;
import com.marvin.grocery.dto.ArticleGroupDTO;
import com.marvin.grocery.dto.ArticleGroupRequest;
import com.marvin.grocery.dto.UpdateArticleGroupAssignmentRequest;
import com.marvin.grocery.service.ArticleManagementService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleController Tests")
class ArticleControllerTest {

    @Mock
    private ArticleManagementService articleManagementService;

    @InjectMocks
    private ArticleController articleController;

    @Test
    @DisplayName("Should return all articles as a Flux")
    void listArticles_ReturnsFluxOfArticles() {
        final ArticleDTO dto = new ArticleDTO(1L, "vollmilch", "Vollmilch", 3L, "Dairy", 5L);
        when(articleManagementService.findAllArticles()).thenReturn(Flux.just(dto));

        final Flux<ArticleDTO> result = articleController.listArticles();

        StepVerifier.create(result)
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 200 with the updated article after assigning a group")
    void assignGroup_ArticleAndGroupExist_Returns200() {
        final ArticleDTO dto = new ArticleDTO(1L, "vollmilch", "Vollmilch", 3L, "Dairy", 5L);
        when(articleManagementService.assignGroup(1L, 3L)).thenReturn(Mono.just(dto));

        final Mono<ResponseEntity<ArticleDTO>> result =
                articleController.assignGroup(1L, new UpdateArticleGroupAssignmentRequest(3L));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals(1L, response.getBody().id());
                    assertEquals(3L, response.getBody().groupId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 404 when the article does not exist")
    void assignGroup_ArticleNotFound_Returns404() {
        when(articleManagementService.assignGroup(eq(99L), eq(3L)))
                .thenReturn(Mono.error(new NoSuchElementException("Article not found: 99")));

        final Mono<ResponseEntity<ArticleDTO>> result =
                articleController.assignGroup(99L, new UpdateArticleGroupAssignmentRequest(3L));

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 201 with the created group")
    void createGroup_ValidRequest_Returns201() {
        final ArticleGroupDTO dto = new ArticleGroupDTO(3L, "Dairy");
        when(articleManagementService.createGroup("Dairy")).thenReturn(Mono.just(dto));

        final Mono<ResponseEntity<ArticleGroupDTO>> result =
                articleController.createGroup(new ArticleGroupRequest("Dairy"));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Dairy", response.getBody().name());
                })
                .verifyComplete();

        verify(articleManagementService).createGroup("Dairy");
    }

    @Test
    @DisplayName("Should return 200 with the renamed group")
    void renameGroup_GroupExists_Returns200() {
        final ArticleGroupDTO dto = new ArticleGroupDTO(3L, "Milk Products");
        when(articleManagementService.renameGroup(3L, "Milk Products")).thenReturn(Mono.just(dto));

        final Mono<ResponseEntity<ArticleGroupDTO>> result =
                articleController.renameGroup(3L, new ArticleGroupRequest("Milk Products"));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("Milk Products", response.getBody().name());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 404 when renaming an unknown group")
    void renameGroup_GroupNotFound_Returns404() {
        when(articleManagementService.renameGroup(eq(99L), eq("New Name")))
                .thenReturn(Mono.error(new NoSuchElementException("Article group not found: 99")));

        final Mono<ResponseEntity<ArticleGroupDTO>> result =
                articleController.renameGroup(99L, new ArticleGroupRequest("New Name"));

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 204 after successful group deletion")
    void deleteGroup_GroupExists_Returns204() {
        when(articleManagementService.deleteGroup(3L)).thenReturn(Mono.empty());

        final Mono<ResponseEntity<Void>> result = articleController.deleteGroup(3L);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 404 when deleting an unknown group")
    void deleteGroup_GroupNotFound_Returns404() {
        when(articleManagementService.deleteGroup(99L))
                .thenReturn(Mono.error(new NoSuchElementException("Article group not found: 99")));

        final Mono<ResponseEntity<Void>> result = articleController.deleteGroup(99L);

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }
}
