package com.solvestack.dto;

import java.util.List;

public record CatalogResponse(long totalProblems, List<TopicSummary> topics) {}
