package com.solvestack.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.solvestack.config.SecurityConfig;
import com.solvestack.dto.CatalogResponse;
import com.solvestack.dto.ProblemResponse;
import com.solvestack.dto.TopicResponse;
import com.solvestack.dto.TopicSummary;
import com.solvestack.exception.TopicNotFoundException;
import com.solvestack.service.CatalogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CatalogController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class CatalogControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogService catalog;

    @Test
    void servesPublicCatalogAndTopicAsJson() throws Exception {
        given(catalog.getCatalog()).willReturn(new CatalogResponse(
                1, List.of(new TopicSummary("graphs", "Graphs", "green", 1))));
        given(catalog.getTopic("graphs")).willReturn(new TopicResponse("graphs", "Graphs", "green",
                List.of(new ProblemResponse("graph-1", "Example", "https://example.com/problem", 1))));

        mvc.perform(get("/api/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProblems").value(1))
                .andExpect(jsonPath("$.topics[0].slug").value("graphs"))
                .andExpect(jsonPath("$.topics[0].problemCount").value(1));

        mvc.perform(get("/api/topics/graphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problems[0].id").value("graph-1"))
                .andExpect(jsonPath("$.problems[0].position").value(1));
    }

    @Test
    void returnsProblemDetailsWhenTopicIsMissing() throws Exception {
        given(catalog.getTopic("missing")).willThrow(new TopicNotFoundException());

        mvc.perform(get("/api/topics/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Topic not found"));
    }

    @Test
    void rejectsCatalogWritesAndUnconfiguredRoutes() throws Exception {
        mvc.perform(post("/api/topics")).andExpect(status().isForbidden());
        mvc.perform(get("/api/me/completions")).andExpect(status().isForbidden());
    }
}
