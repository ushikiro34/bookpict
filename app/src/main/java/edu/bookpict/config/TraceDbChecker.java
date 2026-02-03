package edu.bookpict.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TraceDbChecker {

    private static final Pattern SYNTAX_ERROR = Pattern.compile("Syntax error in SQL statement", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_LOCK = Pattern.compile("The file is locked", Pattern.CASE_INSENSITIVE);

    @EventListener(ApplicationReadyEvent.class)
    public void checkTraceDb() {
        try {
            File trace = Paths.get("app", "data", "bookpict.trace.db").toFile();
            if (!trace.exists()) return;

            try (BufferedReader r = new BufferedReader(new FileReader(trace))) {
                String line;
                int syntaxCount = 0;
                int lockCount = 0;
                while ((line = r.readLine()) != null) {
                    if (SYNTAX_ERROR.matcher(line).find()) syntaxCount++;
                    if (FILE_LOCK.matcher(line).find()) lockCount++;
                }

                if (syntaxCount > 0 || lockCount > 0) {
                    log.warn("Detected {} syntax error(s) and {} file-lock warning(s) in bookpict.trace.db - review H2 console usage or scripts.", syntaxCount, lockCount);
                }
            }
        } catch (Exception e) {
            log.debug("TraceDbChecker failed: {}", e.getMessage());
        }
    }
}
