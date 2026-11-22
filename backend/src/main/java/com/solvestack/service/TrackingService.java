package com.solvestack.service;

import com.solvestack.dto.CatalogResponse;
import com.solvestack.dto.CompletionResult;
import com.solvestack.dto.ProgressStats;
import com.solvestack.dto.TopicResponse;
import com.solvestack.dto.TrackedTopic;
import com.solvestack.dto.TrackedTopic.TrackedProblem;
import com.solvestack.dto.TrackerProgress;
import com.solvestack.dto.TrackerProgress.TopicProgress;
import com.solvestack.entity.TrackerOwner;
import com.solvestack.exception.ProblemNotFoundException;
import com.solvestack.repository.CompletionRepository;
import com.solvestack.repository.CompletionRepository.TopicCount;
import com.solvestack.repository.ProblemRepository;
import com.solvestack.repository.TrackerOwnerRepository;
import com.solvestack.security.TrackerPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrackingService {
    private final CatalogService catalog;
    private final ProblemRepository problems;
    private final CompletionRepository completions;
    private final TrackerOwnerRepository owners;

    public TrackingService(CatalogService catalog, ProblemRepository problems,
            CompletionRepository completions, TrackerOwnerRepository owners) {
        this.catalog = catalog;
        this.problems = problems;
        this.completions = completions;
        this.owners = owners;
    }

    public TrackerProgress getProgress(TrackerPrincipal principal) {
        UUID ownerId = verifyOwner(principal, owners.findForShare(principal.ownerId()));
        CatalogResponse catalogResponse = catalog.getCatalog();
        Map<String, Long> counts = completions.countByTopic(ownerId).stream()
                .collect(Collectors.toMap(TopicCount::getSlug, TopicCount::getCompleted));

        List<TopicProgress> topics = catalogResponse.topics().stream()
                .map(topic -> new TopicProgress(topic.slug(), topic.name(), topic.accent(),
                        ProgressStats.from(counts.getOrDefault(topic.slug(), 0L), topic.problemCount())))
                .toList();
        long completed = topics.stream().mapToLong(topic -> topic.progress().completedProblems()).sum();
        return new TrackerProgress(ProgressStats.from(completed, catalogResponse.totalProblems()), topics);
    }

    public TrackedTopic getTopic(TrackerPrincipal principal, String slug) {
        UUID ownerId = verifyOwner(principal, owners.findForShare(principal.ownerId()));
        TopicResponse topic = catalog.getTopic(slug);
        Set<String> completedIds = completions.findCompletedProblemIds(ownerId);
        List<TrackedProblem> tracked = topic.problems().stream()
                .map(problem -> TrackedProblem.from(problem, completedIds.contains(problem.id())))
                .toList();

        long completedInTopic = tracked.stream().filter(TrackedProblem::completed).count();
        return new TrackedTopic(topic.slug(), topic.name(), topic.accent(),
                ProgressStats.from(completedInTopic, tracked.size()),
                ProgressStats.from(completedIds.size(), problems.count()), tracked);
    }

    public CompletionResult setCompletion(TrackerPrincipal principal, String problemId, boolean completed) {
        UUID ownerId = verifyOwner(principal, owners.findForUpdate(principal.ownerId()));
        if (!problems.existsById(problemId)) {
            throw new ProblemNotFoundException();
        }

        if (completed) {
            completions.markDone(ownerId, problemId);
        } else {
            completions.undo(ownerId, problemId);
        }

        ProgressStats overall = ProgressStats.from(completions.countByOwner(ownerId), problems.count());
        return new CompletionResult(problemId, completed, overall);
    }

    public void reset(TrackerPrincipal principal) {
        UUID ownerId = verifyOwner(principal, owners.findForUpdate(principal.ownerId()));
        completions.reset(ownerId);
    }

    // Check the session kind under the same row lock used by account upgrades.
    private static UUID verifyOwner(TrackerPrincipal principal, Optional<TrackerOwner> owner) {
        return owner.filter(tracker -> tracker.getKind() == principal.kind())
                .map(TrackerOwner::getId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("An active session is required."));
    }
}
