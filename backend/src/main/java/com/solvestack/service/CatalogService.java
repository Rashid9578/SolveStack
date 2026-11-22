package com.solvestack.service;

import com.solvestack.dto.CatalogResponse;
import com.solvestack.dto.TopicResponse;
import com.solvestack.dto.TopicSummary;
import com.solvestack.exception.TopicNotFoundException;
import com.solvestack.repository.ProblemRepository;
import com.solvestack.repository.TopicRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final TopicRepository topics;
    private final ProblemRepository problems;

    public CatalogService(TopicRepository topics, ProblemRepository problems) {
        this.topics = topics;
        this.problems = problems;
    }

    public CatalogResponse getCatalog() {
        List<TopicSummary> catalog = topics.findCatalog();
        long total = catalog.stream().mapToLong(TopicSummary::problemCount).sum();
        return new CatalogResponse(total, catalog);
    }

    public TopicResponse getTopic(String slug) {
        TopicSummary topic = topics.findSummaryBySlug(slug).orElseThrow(TopicNotFoundException::new);
        return new TopicResponse(topic.slug(), topic.name(), topic.accent(), problems.findByTopicSlug(slug));
    }
}
