package com.marvin.grocery.entity;

import com.marvin.costs.entity.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a proposed (or already applied) {@link ArticleGroupEntity} assignment
 * for an {@link ArticleEntity}, produced either by the local heuristic matcher or by the LLM matcher.
 */
@Getter
@Setter
@Entity
@Table(name = "article_group_suggestion", schema = "grocery")
public class ArticleGroupSuggestionEntity extends BasicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "article_group_suggestion_id_gen")
    @SequenceGenerator(
            name = "article_group_suggestion_id_gen",
            sequenceName = "grocery.article_group_suggestion_id_seq",
            allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private ArticleEntity article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_group_id", nullable = false)
    private ArticleGroupEntity suggestedGroup;

    @Column(name = "score", nullable = false)
    private double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SuggestionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SuggestionStatus status;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ArticleGroupSuggestionEntity that = (ArticleGroupSuggestionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
