package edu.bookpict.web.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IsbnInfoDto {
    private String isbn;
    private String publicationDate;
    private String coverType;
    private Integer pageCount;
    private List<EditionInfoDto> editions;
}
