package edu.bookpict.aladin.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.bookpict.aladin.client.AladinResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AladinMapper {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

    public AladinResponseDto parseResponse(String json) {
        try {
            // Aladin JSON usually has a semicolon at the end or is wrapped in a callback,
            // but for output=js it usually returns a plain JSON object.
            // If it's wrapped, we might need to strip it.
            return objectMapper.readValue(json, AladinResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to parse Aladin API response: {}", e.getMessage());
            return null;
        }
    }
}
