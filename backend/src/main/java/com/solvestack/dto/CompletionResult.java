package com.solvestack.dto;

public record CompletionResult(String problemId, boolean completed, ProgressStats overall) {}
