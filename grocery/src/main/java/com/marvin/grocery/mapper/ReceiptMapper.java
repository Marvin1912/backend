package com.marvin.grocery.mapper;

import com.marvin.grocery.dto.ReceiptDTO;
import com.marvin.grocery.dto.ReceiptItemDTO;
import com.marvin.grocery.entity.ReceiptEntity;
import com.marvin.grocery.entity.ReceiptItemEntity;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapper for converting between receipt entities and DTOs. */
@Mapper(componentModel = "spring")
public interface ReceiptMapper {

    /**
     * Maps a receipt entity to a DTO without items, computing total from items on the entity.
     *
     * @param entity the receipt entity to map
     * @return the receipt DTO with totalAmount but without items list
     */
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "totalAmount", expression = "java(computeTotal(entity))")
    ReceiptDTO toReceiptDTO(ReceiptEntity entity);

    /**
     * Maps a receipt entity to a DTO including the full items list.
     *
     * @param entity the receipt entity to map
     * @return the receipt DTO with totalAmount and items populated
     */
    @Mapping(target = "totalAmount", expression = "java(computeTotal(entity))")
    @Mapping(target = "items", source = "items")
    ReceiptDTO toReceiptDTOWithItems(ReceiptEntity entity);

    /**
     * Maps a receipt item entity to a DTO.
     *
     * @param entity the receipt item entity to map
     * @return the receipt item DTO
     */
    ReceiptItemDTO toReceiptItemDTO(ReceiptItemEntity entity);

    /**
     * Maps a list of receipt item entities to DTOs.
     *
     * @param entities the list of receipt item entities
     * @return the list of receipt item DTOs
     */
    List<ReceiptItemDTO> toReceiptItemDTOList(List<ReceiptItemEntity> entities);

    /**
     * Computes the total amount for a receipt by summing all item prices.
     *
     * @param entity the receipt entity whose items to sum
     * @return the total amount, or zero if there are no items
     */
    default BigDecimal computeTotal(ReceiptEntity entity) {
        return entity.getItems().stream()
                .map(ReceiptItemEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
