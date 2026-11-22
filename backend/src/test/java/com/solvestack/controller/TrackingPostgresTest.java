package com.solvestack.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solvestack.dto.CompletionResult;
import com.solvestack.dto.ProgressStats;
import com.solvestack.dto.TrackedTopic;
import com.solvestack.dto.TrackerProgress;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "server.address=127.0.0.1", "solvestack.session.cookie-secure=true" })
@EnabledIfEnvironmentVariable(named = "SOLVESTACK_POSTGRES_TEST", matches = "true")
class TrackingPostgresTest {
    private static final String PROGRESS = "/api/me/progress";
    private static final String TOPIC = "/api/me/topics/array-and-string";
    private static final String COMPLETIONS = "/api/me/completions";

    @Autowired
    private ObjectMapper json;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void requiresAnOwnerSessionAndCsrfForEveryWrite() throws Exception {
        ApiBrowser anonymous = browser();
        assertThat(anonymous.get("/api/topics").statusCode()).isEqualTo(200);
        expectProblem(anonymous.get(PROGRESS), 401, "Unauthorized");
        expectProblem(anonymous.get(TOPIC), 401, "Unauthorized");
        expectProblem(anonymous.write("PUT", completion("arr-1")), 401, "Unauthorized");
        expectProblem(anonymous.write("DELETE", completion("arr-1")), 401, "Unauthorized");
        expectProblem(anonymous.write("DELETE", COMPLETIONS), 401, "Unauthorized");

        ApiBrowser guest = guest();
        for (String method : List.of("PUT", "DELETE")) {
            expectProblem(guest.send(guest.request(method, completion("arr-1"), null, null)), 403, "Forbidden");
            expectProblem(guest.write(method, completion("missing")), 404, "Problem not found");
        }
        expectProblem(guest.send(guest.request("DELETE", COMPLETIONS, null, null)), 403, "Forbidden");
        expectProblem(guest.get("/api/me/topics/missing"), 404, "Topic not found");
        assertThat(progress(guest).overall()).isEqualTo(new ProgressStats(0, 100, 0, false));
    }

    @Test
    void updatesOrderedProgressIdempotentlyAndKeepsTrackersIsolatedAcrossAccountUpgrade() throws Exception {
        ApiBrowser guest = guest();
        ApiBrowser other = guest();
        var initial = progress(guest);
        assertThat(initial.topics()).hasSize(12);
        assertThat(initial.topics().getFirst().slug()).isEqualTo("array-and-string");
        assertThat(initial.topics().getLast().slug()).isEqualTo("graphs");

        var firstMark = change(guest.write("PUT", completion("arr-1")));
        assertThat(firstMark.completed()).isTrue();
        assertThat(firstMark.overall()).isEqualTo(new ProgressStats(1, 100, 1, false));
        assertThat(change(guest.write("PUT", completion("arr-1")))).isEqualTo(firstMark);
        assertThat(progress(other).overall().completedProblems()).isZero();

        var topic = read(guest.get(TOPIC), 200, TrackedTopic.class);
        assertThat(topic.progress()).isEqualTo(new ProgressStats(1, 23, 4, false));
        assertThat(topic.overall()).isEqualTo(firstMark.overall());
        assertThat(topic.problems().getFirst().id()).isEqualTo("arr-1");
        assertThat(topic.problems()).filteredOn(TrackedTopic.TrackedProblem::completed).hasSize(1);
        assertThat(progress(guest).topics().getFirst().progress()).isEqualTo(topic.progress());

        change(other.write("PUT", completion("arr-1")));
        var undone = change(guest.write("DELETE", completion("arr-1")));
        assertThat(undone.completed()).isFalse();
        assertThat(undone.overall().completedProblems()).isZero();
        assertThat(change(guest.write("DELETE", completion("arr-1")))).isEqualTo(undone);
        assertThat(progress(other).overall().completedProblems()).isEqualTo(1);

        change(guest.write("PUT", completion("arr-2")));
        String oldGuestCookie = guest.cookie;
        Map<String, String> credentials = Map.of(
                "email", UUID.randomUUID() + "@example.net", "password", "example-passphrase-123");
        assertThat(guest.mutate("/api/session/register", credentials).statusCode()).isEqualTo(201);
        var upgraded = read(guest.get(TOPIC), 200, TrackedTopic.class);
        assertThat(upgraded.problems()).filteredOn(TrackedTopic.TrackedProblem::completed)
                .extracting(TrackedTopic.TrackedProblem::id).containsExactly("arr-2");

        ApiBrowser newBrowser = browser();
        newBrowser.cookie = oldGuestCookie;
        expectProblem(newBrowser.get(PROGRESS), 401, "Unauthorized");
        assertThat(newBrowser.mutate("/api/session/login", credentials).statusCode()).isEqualTo(200);
        assertThat(progress(newBrowser)).isEqualTo(progress(guest));

        assertThat(newBrowser.write("DELETE", COMPLETIONS).statusCode()).isEqualTo(204);
        assertThat(newBrowser.write("DELETE", COMPLETIONS).statusCode()).isEqualTo(204);
        assertThat(progress(guest).overall().completedProblems()).isZero();
        assertThat(progress(other).overall().completedProblems()).isEqualTo(1);
    }

    @Test
    void serializesSimultaneousUpdatesAndResetForTheSameTracker() throws Exception {
        ApiBrowser guest = guest();
        JsonNode csrf = guest.csrf();
        var first = guest.sendAsync(guest.request("PUT", completion("arr-1"), null, csrf));
        var second = guest.sendAsync(guest.request("PUT", completion("arr-2"), null, csrf));
        var retry = guest.sendAsync(guest.request("PUT", completion("arr-1"), null, csrf));
        change(first.get(20, TimeUnit.SECONDS));
        change(second.get(20, TimeUnit.SECONDS));
        change(retry.get(20, TimeUnit.SECONDS));
        assertThat(progress(guest).overall().completedProblems()).isEqualTo(2);

        var reset = guest.sendAsync(guest.request("DELETE", COMPLETIONS, null, csrf));
        var mark = guest.sendAsync(guest.request("PUT", completion("arr-3"), null, csrf));
        assertThat(reset.get(20, TimeUnit.SECONDS).statusCode()).isEqualTo(204);
        long afterMark = change(mark.get(20, TimeUnit.SECONDS)).overall().completedProblems();
        assertThat(afterMark).isIn(1L, 3L);

        var topic = read(guest.get(TOPIC), 200, TrackedTopic.class);
        var completedIds = topic.problems().stream().filter(TrackedTopic.TrackedProblem::completed)
                .map(TrackedTopic.TrackedProblem::id).toList();
        assertThat(completedIds).isEqualTo(afterMark == 1 ? List.of("arr-3") : List.of());
    }

    @Test
    void signalsCompletionOnlyAfterTheLastProblemAndClearsItAfterUndo() throws Exception {
        ApiBrowser guest = guest();
        List<String> ids = allProblemIds(guest);
        assertThat(ids).hasSize(100).doesNotHaveDuplicates();

        JsonNode csrf = guest.csrf();
        for (String id : ids.subList(0, 99)) {
            change(guest.send(guest.request("PUT", completion(id), null, csrf)));
        }
        assertThat(progress(guest).overall()).isEqualTo(new ProgressStats(99, 100, 99, false));

        String last = completion(ids.getLast());
        var finished = change(guest.write("PUT", last));
        assertThat(finished.overall()).isEqualTo(new ProgressStats(100, 100, 100, true));
        var tracked = progress(guest);
        assertThat(tracked.overall()).isEqualTo(finished.overall());
        assertThat(tracked.topics()).allSatisfy(topic -> assertThat(topic.progress().complete()).isTrue());

        assertThat(change(guest.write("DELETE", last)).overall()).isEqualTo(new ProgressStats(99, 100, 99, false));
        assertThat(guest.write("DELETE", COMPLETIONS).statusCode()).isEqualTo(204);
        assertThat(progress(guest).overall()).isEqualTo(new ProgressStats(0, 100, 0, false));
    }

    private List<String> allProblemIds(ApiBrowser guest) throws Exception {
        List<String> ids = new ArrayList<>();
        for (var topic : progress(guest).topics()) {
            var response = guest.get("/api/topics/" + topic.slug());
            assertThat(response.statusCode()).isEqualTo(200);
            for (var problem : json.readTree(response.body()).path("problems")) {
                ids.add(problem.path("id").asText());
            }
        }
        return ids;
    }

    private ApiBrowser guest() throws Exception {
        ApiBrowser browser = browser();
        assertThat(browser.mutate("/api/session/guest", null).statusCode()).isEqualTo(201);
        return browser;
    }

    private ApiBrowser browser() {
        return new ApiBrowser(http, json, port);
    }

    private TrackerProgress progress(ApiBrowser browser) throws Exception {
        return read(browser.get(PROGRESS), 200, TrackerProgress.class);
    }

    private CompletionResult change(HttpResponse<String> response) throws Exception {
        return read(response, 200, CompletionResult.class);
    }

    private <T> T read(HttpResponse<String> response, int status, Class<T> type) throws Exception {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return json.readValue(response.body(), type);
    }

    private void expectProblem(HttpResponse<String> response, int status, String title) throws Exception {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));
        assertThat(json.readTree(response.body()).path("title").asText()).isEqualTo(title);
    }

    private static String completion(String id) {
        return COMPLETIONS + "/" + id;
    }
}
