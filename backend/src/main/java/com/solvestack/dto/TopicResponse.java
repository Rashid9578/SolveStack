package com.solvestack.dto;

import java.util.List;

public record TopicResponse(String slug, String name, String accent, List<ProblemResponse> problems) {}
