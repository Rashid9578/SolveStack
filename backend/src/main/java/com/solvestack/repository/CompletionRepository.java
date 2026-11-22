package com.solvestack.repository;

import com.solvestack.entity.CompletionId;
import com.solvestack.entity.ProblemCompletion;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface CompletionRepository extends Repository<ProblemCompletion, CompletionId> {
    @Query("""
            select c.problem.topic.slug as slug, count(c) as completed
            from ProblemCompletion c
            where c.id.ownerId = :ownerId
            group by c.problem.topic.slug
            """)
    List<TopicCount> countByTopic(UUID ownerId);

    @Query("select c.id.problemId from ProblemCompletion c where c.id.ownerId = :ownerId")
    Set<String> findCompletedProblemIds(UUID ownerId);

    @Query("select count(c) from ProblemCompletion c where c.id.ownerId = :ownerId")
    long countByOwner(UUID ownerId);

    @Modifying
    @Query(value = """
            INSERT INTO problem_completions (owner_id, problem_id)
            VALUES (:ownerId, :problemId)
            ON CONFLICT (owner_id, problem_id) DO NOTHING
            """, nativeQuery = true)
    void markDone(UUID ownerId, String problemId);

    @Modifying
    @Query("delete from ProblemCompletion c where c.id.ownerId = :ownerId and c.id.problemId = :problemId")
    void undo(UUID ownerId, String problemId);

    @Modifying
    @Query("delete from ProblemCompletion c where c.id.ownerId = :ownerId")
    void reset(UUID ownerId);

    interface TopicCount {
        String getSlug();

        long getCompleted();
    }
}
