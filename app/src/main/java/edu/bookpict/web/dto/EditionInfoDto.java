package edu.bookpict.web.dto;

import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditionInfoDto {
    private String editionId;
    private Integer editionNumber;
    private Integer printNumber;
    private Boolean isLatest;
    private List<PriceInfoDto> prices;
}
