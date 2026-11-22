package com.solvestack.dto;

import java.util.List;

public record TrackerProgress(ProgressStats overall, List<TopicProgress> topics) {
    public record TopicProgress(String slug, String name, String accent, ProgressStats progress) {}
}
