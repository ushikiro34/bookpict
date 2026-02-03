package edu.bookpict.aladin.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AladinResponseDto {
    private List<Item> item;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String title;
        private String author;
        private String publisher;
        private String isbn13;
        private Integer priceSales;
        private Integer priceStandard;
        private Integer customerReviewRank;
        private Integer bestRank; // for ItemList (Bestseller)
        private String link;
        private String cover;
        private String categoryName;
        private String description; // summary
        private String fullDescription; // rich summary
        private String toc; // table of contents

        private BestSellerRank bestSellerRank; // for ItemLookUp with OptResult
        private SubInfo subInfo;

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SubInfo {
            private RatingDetail ratingInfo;
            private String toc; // SubInfo might also contain toc in some versions
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class BestSellerRank {
            private Integer rank;
            private String explanation;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RatingDetail {
            private java.math.BigDecimal ratingScore;
            private Integer ratingCount;
        }
    }
}
