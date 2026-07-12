package com.marvin.grocery.mapper;

import com.marvin.grocery.dto.ArticleGroupDTO;
import com.marvin.grocery.entity.ArticleGroupEntity;
import org.mapstruct.Mapper;

/** MapStruct mapper for converting between article group entities and DTOs. */
@Mapper(componentModel = "spring")
public interface ArticleGroupMapper {

    /**
     * Maps an article group entity to a DTO.
     *
     * @param entity the article group entity to map
     * @return the article group DTO
     */
    ArticleGroupDTO toArticleGroupDTO(ArticleGroupEntity entity);
}
