package edu.bookpict.domain.book;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aladin_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AladinSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String isbn13;

    private Integer priceSales;
    private Integer priceStandard;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    private Integer reviewCount;
    private Integer bestsellerRank;

    private String productUrl;

    private LocalDateTime collectedAt;
}
