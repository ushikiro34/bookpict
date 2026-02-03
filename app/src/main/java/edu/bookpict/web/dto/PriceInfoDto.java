package edu.bookpict.web.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceInfoDto {
    private String storeId;
    private String storeName;
    private Integer price;
    private BigDecimal rating;
    private Integer reviewCount;
    private String stockStatus;
    private String productUrl;
}
