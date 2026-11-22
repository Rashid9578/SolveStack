package com.solvestack.repository;

import com.solvestack.dto.ProblemResponse;
import com.solvestack.entity.Problem;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends Repository<Problem, String> {
    boolean existsById(String id);

    long count();

    @Query("""
            select new com.solvestack.dto.ProblemResponse(p.id, p.title, p.externalUrl, p.position)
            from Problem p
            where p.topic.slug = :slug
            order by p.position
            """)
    List<ProblemResponse> findByTopicSlug(@Param("slug") String slug);
}
