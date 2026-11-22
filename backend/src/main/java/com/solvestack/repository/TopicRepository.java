package com.solvestack.repository;

import com.solvestack.dto.TopicSummary;
import com.solvestack.entity.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends Repository<Topic, Long> {
    @Query("""
            select new com.solvestack.dto.TopicSummary(t.slug, t.name, t.accent, count(p.id))
            from Topic t left join t.problems p
            group by t.id, t.slug, t.name, t.accent, t.position
            order by t.position
            """)
    List<TopicSummary> findCatalog();

    @Query("""
            select new com.solvestack.dto.TopicSummary(t.slug, t.name, t.accent, count(p.id))
            from Topic t left join t.problems p
            where t.slug = :slug
            group by t.id, t.slug, t.name, t.accent
            """)
    Optional<TopicSummary> findSummaryBySlug(@Param("slug") String slug);
}
