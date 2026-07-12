package com.marvin.grocery.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marvin.grocery.dto.ArticleGroupDTO;
import com.marvin.grocery.entity.ArticleGroupEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("ArticleGroupMapper Tests")
class ArticleGroupMapperTest {

    private final ArticleGroupMapper articleGroupMapper = Mappers.getMapper(ArticleGroupMapper.class);

    @Test
    @DisplayName("Should map an article group entity to its DTO")
    void toArticleGroupDTO_MapsIdAndName() {
        final ArticleGroupEntity entity = new ArticleGroupEntity();
        entity.setId(3L);
        entity.setName("Dairy");

        final ArticleGroupDTO dto = articleGroupMapper.toArticleGroupDTO(entity);

        assertEquals(3L, dto.id());
        assertEquals("Dairy", dto.name());
    }
}
