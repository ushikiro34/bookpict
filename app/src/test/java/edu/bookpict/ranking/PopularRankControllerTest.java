package edu.bookpict.ranking;

import edu.bookpict.web.controller.PopularRankController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PopularRankControllerTest {

    @Autowired
    private PopularRankController controller;

    @Test
    public void callComputeEndpoint() {
        controller.computeNow();
    }
}
