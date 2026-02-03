package edu.bookpict.service;

import edu.bookpict.domain.book.repository.*;
import edu.bookpict.domain.search.repository.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DbResetService {

    private final BookRepository bookRepository;
    private final IsbnRepository isbnRepository;
    private final EditionRepository editionRepository;
    private final AladinSnapshotRepository aladinSnapshotRepository;
    private final SearchKeywordRepository searchKeywordRepository;

    @Transactional
    public void resetDatabase() {
        log.warn("RESETTING DATABASE...");
        editionRepository.deleteAll();
        aladinSnapshotRepository.deleteAll();
        isbnRepository.deleteAll();
        bookRepository.deleteAll();
        searchKeywordRepository.deleteAll();
        log.warn("DATABASE RESET COMPLETE.");
    }
}
