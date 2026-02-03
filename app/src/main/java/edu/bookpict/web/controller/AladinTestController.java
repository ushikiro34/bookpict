package edu.bookpict.web.controller;

import edu.bookpict.aladin.client.AladinApiClient;
import edu.bookpict.aladin.scheduler.AladinSyncScheduler;
import edu.bookpict.aladin.scheduler.OneTimeElementarySyncScheduler;
import edu.bookpict.aladin.service.AladinBookService;
import edu.bookpict.service.DbResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/aladin")
@RequiredArgsConstructor
public class AladinTestController {

    private final AladinApiClient aladinApiClient;
    private final AladinBookService aladinBookService;
    private final AladinSyncScheduler aladinSyncScheduler;
    private final OneTimeElementarySyncScheduler oneTimeElementarySyncScheduler;
    private final DbResetService dbResetService;

    @PostMapping("/reset")
    public String reset() {
        dbResetService.resetDatabase();
        return "Database reset complete.";
    }

    @PostMapping("/seed")
    public String seed(@RequestParam(defaultValue = "중등 수학 쎈") String query) {
        aladinBookService.importBooksFromSearch(query);
        return "Imported books for query: " + query;
    }

    @PostMapping("/trigger-sync")
    public String triggerSync() {
        aladinSyncScheduler.manualSync();
        return "Manual sync completed.";
    }

    @PostMapping("/full-sync")
    public String fullSync(
            @RequestParam(defaultValue = "초등") String schoolLevel,
            @RequestParam String subject) {
        oneTimeElementarySyncScheduler.manualFullSync(schoolLevel, subject);
        return "Full sync completed for: " + schoolLevel + " " + subject;
    }

    @GetMapping("/search")
    public String search(
            @RequestParam String query,
            @RequestParam(defaultValue = "Title") String queryType) {
        return aladinApiClient.searchItems(query, queryType, 10, 1);
    }

    @GetMapping("/lookup")
    public String lookup(@RequestParam String isbn) {
        return aladinApiClient.lookUpItem(isbn, "ISBN13");
    }

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "Bestseller") String queryType,
            @RequestParam(defaultValue = "0") String categoryId) {
        return aladinApiClient.getItemList(queryType, 10, 1, categoryId);
    }
}
