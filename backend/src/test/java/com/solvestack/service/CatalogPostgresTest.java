package com.solvestack.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.solvestack.dto.CatalogResponse;
import com.solvestack.dto.ProblemResponse;
import com.solvestack.dto.TopicSummary;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.address=127.0.0.1")
@EnabledIfEnvironmentVariable(named = "SOLVESTACK_POSTGRES_TEST", matches = "true")
class CatalogPostgresTest {
    @Autowired
    private CatalogService catalog;

    @Autowired
    private TestRestTemplate http;

    @Test
    void migratesAndReadsTheOrderedCatalog() {
        CatalogResponse response = catalog.getCatalog();
        assertThat(response.totalProblems()).isEqualTo(100);
        assertThat(response.topics()).extracting(TopicSummary::slug).containsExactly(
                "array-and-string", "greedy", "dp", "binary-search",
                "heaps", "recursion", "linked-list", "binary-tree",
                "binary-search-tree", "stack-and-queue", "backtracking", "graphs");
        assertThat(response.topics()).extracting(TopicSummary::problemCount).containsExactly(
                23L, 5L, 13L, 7L, 5L, 6L, 8L, 8L, 6L, 7L, 6L, 6L);

        List<String> problemIds = new ArrayList<>();
        for (TopicSummary topic : response.topics()) {
            List<ProblemResponse> problems = catalog.getTopic(topic.slug()).problems();
            assertThat(problems).hasSize((int) topic.problemCount());
            for (int index = 0; index < problems.size(); index++) {
                assertThat(problems.get(index).position()).isEqualTo(index + 1);
                problemIds.add(problems.get(index).id());
            }
        }
        assertThat(problemIds).hasSize(100).doesNotHaveDuplicates();
        assertThat(problemIds.getFirst()).isEqualTo("arr-1");
        assertThat(problemIds.getLast()).isEqualTo("graph-6");

        List<ProblemResponse> arrays = catalog.getTopic("array-and-string").problems();
        assertThat(arrays.get(1).externalUrl()).isEqualTo(arrays.get(13).externalUrl());
        assertThat(arrays.get(1).id()).isNotEqualTo(arrays.get(13).id());

        var apiCatalog = http.getForEntity("/api/topics", CatalogResponse.class);
        assertThat(apiCatalog.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiCatalog.getBody()).isEqualTo(response);
    }
}
