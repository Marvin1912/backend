package com.marvin.grocery.controller;

import com.marvin.grocery.dto.ArticleDTO;
import com.marvin.grocery.dto.ArticleGroupDTO;
import com.marvin.grocery.dto.ArticleGroupRequest;
import com.marvin.grocery.dto.UpdateArticleGroupAssignmentRequest;
import com.marvin.grocery.service.ArticleManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing grocery articles and article groups, as groundwork for a future
 * frontend management GUI.
 */
@RestController("groceryArticleController")
@Tag(name = "Grocery Articles", description = "Manage grocery articles and their group assignments")
public class ArticleController {

    private final ArticleManagementService articleManagementService;

    /**
     * Creates a new ArticleController with the required service.
     *
     * @param articleManagementService the service handling article and article group management
     */
    public ArticleController(ArticleManagementService articleManagementService) {
        this.articleManagementService = articleManagementService;
    }

    /**
     * Lists all articles with their group assignment and purchase count.
     *
     * @return a Flux emitting all article DTOs
     */
    @GetMapping("/articles")
    @Operation(
            summary = "List all articles",
            description = "Returns every article with its normalized name, display name, group assignment, and "
                    + "the number of receipt items referencing it.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Article list returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArticleDTO.class)))
                )
            }
    )
    public Flux<ArticleDTO> listArticles() {
        return articleManagementService.findAllArticles();
    }

    /**
     * Assigns the given article to a group, or clears the assignment when groupId is null.
     *
     * @param id      the id of the article to update
     * @param request the group assignment payload
     * @return a Mono with 200 OK and the updated article, or 404 if the article or group is not found
     */
    @PatchMapping("/articles/{id}/group")
    @Operation(
            summary = "Assign or clear an article's group",
            description = "Sets the article's group to the given groupId, or clears it when groupId is null.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Article updated",
                        content = @Content(schema = @Schema(implementation = ArticleDTO.class))
                ),
                @ApiResponse(responseCode = "404", description = "Article or article group not found")
            }
    )
    public Mono<ResponseEntity<ArticleDTO>> assignGroup(
            @PathVariable @Parameter(description = "Id of the article") Long id,
            @RequestBody UpdateArticleGroupAssignmentRequest request) {
        return articleManagementService.assignGroup(id, request.groupId())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Creates a new article group.
     *
     * @param request the fields for the new group
     * @return a Mono with 201 Created and the new group
     */
    @PostMapping("/article-groups")
    @Operation(
            summary = "Create an article group",
            description = "Creates a new article group with the given name.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Article group created",
                        content = @Content(schema = @Schema(implementation = ArticleGroupDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Invalid request body (validation failed)")
            }
    )
    public Mono<ResponseEntity<ArticleGroupDTO>> createGroup(@Valid @RequestBody ArticleGroupRequest request) {
        return articleManagementService.createGroup(request.name())
                .map(group -> ResponseEntity.status(HttpStatus.CREATED).body(group));
    }

    /**
     * Renames an existing article group.
     *
     * @param id      the id of the group to rename
     * @param request the new name
     * @return a Mono with 200 OK and the updated group, or 404 if not found
     */
    @PutMapping("/article-groups/{id}")
    @Operation(
            summary = "Rename an article group",
            description = "Updates the display name of an existing article group.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Article group updated",
                        content = @Content(schema = @Schema(implementation = ArticleGroupDTO.class))
                ),
                @ApiResponse(responseCode = "400", description = "Invalid request body (validation failed)"),
                @ApiResponse(responseCode = "404", description = "Article group not found")
            }
    )
    public Mono<ResponseEntity<ArticleGroupDTO>> renameGroup(
            @PathVariable @Parameter(description = "Id of the article group") Long id,
            @Valid @RequestBody ArticleGroupRequest request) {
        return articleManagementService.renameGroup(id, request.name())
                .map(ResponseEntity::ok)
                .onErrorReturn(NoSuchElementException.class, ResponseEntity.notFound().build());
    }

    /**
     * Deletes an article group, decoupling (not cascade-deleting) any articles assigned to it.
     *
     * @param id the id of the group to delete
     * @return a Mono with 204 No Content on success, or 404 Not Found if the group does not exist
     */
    @DeleteMapping("/article-groups/{id}")
    @Operation(
            summary = "Delete an article group",
            description = "Deletes the group and clears the group assignment on any articles that referenced it; "
                    + "articles and receipt items are never cascade-deleted.",
            responses = {
                @ApiResponse(responseCode = "204", description = "Article group deleted"),
                @ApiResponse(responseCode = "404", description = "Article group not found")
            }
    )
    public Mono<ResponseEntity<Void>> deleteGroup(
            @PathVariable @Parameter(description = "Id of the article group to delete") Long id) {
        final ResponseEntity<Void> noContent = ResponseEntity.<Void>noContent().build();
        final ResponseEntity<Void> notFound = ResponseEntity.<Void>notFound().build();
        return articleManagementService.deleteGroup(id)
                .thenReturn(noContent)
                .onErrorReturn(NoSuchElementException.class, notFound);
    }
}
