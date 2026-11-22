package com.solvestack.dto;

public record ProgressStats(long completedProblems, long totalProblems, int percentage, boolean complete) {
    public static ProgressStats from(long completed, long total) {
        int percentage = total == 0 ? 0 : (int) Math.round(100.0 * completed / total);
        return new ProgressStats(completed, total, percentage, total > 0 && completed == total);
    }
}
