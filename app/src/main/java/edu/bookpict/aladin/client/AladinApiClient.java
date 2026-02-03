package edu.bookpict.aladin.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AladinApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${aladin.api.key}")
    private String ttbKey;

    @Value("${aladin.api.base-url}")
    private String baseUrl;

    private static final String VERSION = "20131101";
    private static final String OUTPUT = "js";
    private static final String COVER = "Big";

    /**
     * ItemSearch API (도서 검색)
     */
    public String searchItems(String query, String queryType, int maxResults, int start) {
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/ItemSearch.aspx")
                .queryParam("ttbkey", ttbKey)
                .queryParam("Query", query)
                .queryParam("QueryType", queryType)
                .queryParam("MaxResults", maxResults)
                .queryParam("Start", start)
                .queryParam("Sort", "SalesPoint")
                .queryParam("SearchTarget", "Book")
                .queryParam("output", OUTPUT)
                .queryParam("Version", VERSION)
                .queryParam("Cover", COVER)
                .queryParam("OptResult", "bestSellerRank,ratingInfo,authors,fulldescription,Toc,categoryIdList")
                .build()
                .toUri();

        log.debug("Aladin ItemSearch API call: {}", uri);
        return restTemplate.getForObject(uri, String.class);
    }

    /**
     * ItemLookUp API (도서 상세/ISBN 조회)
     */
    public String lookUpItem(String itemId, String itemIdType) {
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/ItemLookUp.aspx")
                .queryParam("ttbkey", ttbKey)
                .queryParam("itemId", itemId)
                .queryParam("itemIdType", itemIdType)
                .queryParam("output", OUTPUT)
                .queryParam("Version", VERSION)
                .queryParam("Cover", COVER)
                .queryParam("OptResult", "bestSellerRank,ratingInfo,authors,fulldescription,Toc,categoryIdList")
                .build()
                .toUri();

        log.debug("Aladin ItemLookUp API call: {}", uri);
        
        String response = restTemplate.getForObject(uri, String.class);

        log.info("===== ALADIN RAW JSON =====");
        log.info(response);
        log.info("===== END =====");        
        
        return restTemplate.getForObject(uri, String.class);
    }

    /**
     * ItemList API (리스트 조회 - 베스트셀러 등)
     */
    public String getItemList(String queryType, int maxResults, int start, String categoryId) {
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/ItemList.aspx")
                .queryParam("ttbkey", ttbKey)
                .queryParam("QueryType", queryType)
                .queryParam("MaxResults", maxResults)
                .queryParam("Start", start)
                .queryParam("SearchTarget", "Book")
                .queryParam("CategoryId", categoryId)
                .queryParam("output", OUTPUT)
                .queryParam("Version", VERSION)
                .queryParam("Cover", COVER)
                .build()
                .toUri();

        log.debug("Aladin ItemList API call: {}", uri);
        return restTemplate.getForObject(uri, String.class);
    }
}
