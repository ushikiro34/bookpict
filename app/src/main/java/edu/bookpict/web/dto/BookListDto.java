package edu.bookpict.web.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookListDto {
    private String bookId;
    private String title;
    private String publisher;
    private String subject;
    private String schoolLevel;
    private String grade;
    private String semester;
    private String thumbnailUrl;

    private Integer price;
    private BigDecimal rating;
    private Integer reviewCount;
    private Double bookPictIndex;
    private Integer bestsellerRank;
    private String aladinUrl;

    private Integer latestEditionNumber;
    private String latestIsbn;
    private String curriculum;
    private String stockStatus;
    private String categoryName;
}
