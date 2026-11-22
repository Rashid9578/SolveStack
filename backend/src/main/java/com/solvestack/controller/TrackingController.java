package com.solvestack.controller;

import com.solvestack.dto.CompletionResult;
import com.solvestack.dto.TrackedTopic;
import com.solvestack.dto.TrackerProgress;
import com.solvestack.security.TrackerPrincipal;
import com.solvestack.service.TrackingService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class TrackingController {
    private final TrackingService tracking;

    public TrackingController(TrackingService tracking) {
        this.tracking = tracking;
    }

    @GetMapping("/progress")
    public ResponseEntity<TrackerProgress> progress(@AuthenticationPrincipal TrackerPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tracking.getProgress(principal));
    }

    @GetMapping("/topics/{slug}")
    public ResponseEntity<TrackedTopic> topic(@AuthenticationPrincipal TrackerPrincipal principal, @PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tracking.getTopic(principal, slug));
    }

    @PutMapping("/completions/{problemId}")
    public ResponseEntity<CompletionResult> markDone(
            @AuthenticationPrincipal TrackerPrincipal principal, @PathVariable String problemId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(tracking.setCompletion(principal, problemId, true));
    }

    @DeleteMapping("/completions/{problemId}")
    public ResponseEntity<CompletionResult> undo(
            @AuthenticationPrincipal TrackerPrincipal principal, @PathVariable String problemId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(tracking.setCompletion(principal, problemId, false));
    }

    @DeleteMapping("/completions")
    public ResponseEntity<Void> reset(@AuthenticationPrincipal TrackerPrincipal principal) {
        tracking.reset(principal);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
