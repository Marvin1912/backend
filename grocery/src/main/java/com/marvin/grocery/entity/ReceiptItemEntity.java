package com.marvin.grocery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/** JPA entity representing a single line item on a grocery receipt. */
@Getter
@Setter
@Entity
@Table(name = "receipt_item", schema = "grocery")
public class ReceiptItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "receipt_item_id_gen")
    @SequenceGenerator(name = "receipt_item_id_gen", sequenceName = "grocery.receipt_item_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private ReceiptEntity receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private ArticleEntity article;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "single_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal singlePrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReceiptItemEntity that = (ReceiptItemEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
