package com.solvestack.dto;

import java.util.List;

public record TrackedTopic(String slug, String name, String accent, ProgressStats progress, ProgressStats overall,
        List<TrackedProblem> problems) {

    public record TrackedProblem(String id, String title, String externalUrl, int position, boolean completed) {
        public static TrackedProblem from(ProblemResponse problem, boolean completed) {
            return new TrackedProblem(problem.id(), problem.title(), problem.externalUrl(), problem.position(), completed);
        }
    }
}
