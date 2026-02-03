package edu.bookpict.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AladinMetrics {

    private Integer price;
    private BigDecimal rating;
    private Integer reviewCount;
    private Integer bestsellerRank;
    private String productUrl;
    private Double bookPictIndex;

    public static AladinMetrics empty() {
        return new AladinMetrics(null, null, null, null, null, null);
    }
}
