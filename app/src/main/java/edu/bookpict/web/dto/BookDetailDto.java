package edu.bookpict.web.dto;

import lombok.*;
import java.util.List;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDetailDto {
    private String bookId;
    private String title;
    private String publisher;
    private String subject;
    private String schoolLevel;
    private String grade;
    private String semester;
    private String curriculum;
    private String summary;
    private String toc;
    private String thumbnailUrl;
    private List<IsbnInfoDto> isbns;

    // Aladin metrics
    private BigDecimal rating;
    private Integer reviewCount;
    private Double bookPictIndex;
    private Integer bestsellerRank;    
}
