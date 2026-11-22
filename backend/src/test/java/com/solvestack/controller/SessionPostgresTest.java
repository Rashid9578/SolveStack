package com.solvestack.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "server.address=127.0.0.1", "solvestack.session.cookie-secure=true" })
@EnabledIfEnvironmentVariable(named = "SOLVESTACK_POSTGRES_TEST", matches = "true")
class SessionPostgresTest {
    private static final String PASSWORD = "sample-test-passphrase";
    private static final String COOKIE_NAME = "SOLVESTACK_SESSION=";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate database;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private PasswordEncoder encoder;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void upgradesGuestInPlaceAndInvalidatesCookiesOnUpgradeAndLogout() throws Exception {
        Browser guest = new Browser();
        var anonymous = guest.get("/api/session");
        assertThat(read(anonymous).path("kind").asText()).isEqualTo("ANONYMOUS");
        assertThat(anonymous.headers().firstValue("Set-Cookie")).isEmpty();

        var denied = guest.send(guest.post("/api/session/guest", null, null));
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(denied.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));

        JsonNode oldCsrf = guest.csrf();
        String anonymousCookie = guest.cookie;
        var started = guest.send(guest.post("/api/session/guest", null, oldCsrf));
        assertThat(started.statusCode()).isEqualTo(201);
        assertThat(read(started).path("kind").asText()).isEqualTo("GUEST");
        assertThat(setCookie(started)).contains("Max-Age=31536000", "Secure", "HttpOnly", "SameSite=Lax");
        assertThat(guest.cookie).isNotEqualTo(anonymousCookie);
        assertThat(idleTimeout(guest)).isEqualTo(31_536_000);
        assertThat(guest.send(guest.post("/api/session/guest", null, oldCsrf)).statusCode()).isEqualTo(403);

        Browser other = createGuest();
        UUID guestOwner = ownerOf(guest);
        UUID otherOwner = ownerOf(other);
        assertThat(otherOwner).isNotEqualTo(guestOwner);
        database.update("INSERT INTO problem_completions (owner_id, problem_id) VALUES (?, ?)", guestOwner, "arr-1");
        database.update("INSERT INTO problem_completions (owner_id, problem_id) VALUES (?, ?)", otherOwner, "arr-2");

        String formerGuestCookie = guest.cookie;
        String email = UUID.randomUUID() + "@example.net";
        var registered = guest.mutate("/api/session/register",
                Map.of("email", email.toUpperCase(Locale.ROOT), "password", PASSWORD));
        assertThat(registered.statusCode()).isEqualTo(201);
        assertThat(read(registered).path("email").asText()).isEqualTo(email);
        assertThat(read(registered).path("kind").asText()).isEqualTo("ACCOUNT");
        assertThat(setCookie(registered)).contains("Secure", "HttpOnly", "SameSite=Lax").doesNotContain("Max-Age");
        assertThat(guest.cookie).isNotEqualTo(formerGuestCookie);
        assertThat(idleTimeout(guest)).isEqualTo(1_800);
        assertThat(ownerOf(guest)).isEqualTo(guestOwner);
        assertThat(completions(guestOwner)).containsExactly("arr-1");
        assertThat(completions(otherOwner)).containsExactly("arr-2");

        String hash = database.queryForObject(
                "SELECT password_hash FROM tracker_owners WHERE id = ?", String.class, guestOwner);
        assertThat(hash).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, hash)).isTrue();
        assertThat(kindFor(formerGuestCookie)).isEqualTo("ANONYMOUS");
        assertThat(kindFor(other.cookie)).isEqualTo("GUEST");

        var duplicate = other.mutate("/api/session/register", Map.of("email", email, "password", PASSWORD));
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(ownerOf(other)).isEqualTo(otherOwner);
        assertThat(kindFor(other.cookie)).isEqualTo("GUEST");

        String accountCookie = guest.cookie;
        var signedOut = guest.mutate("/api/session/logout", null);
        assertThat(signedOut.statusCode()).isEqualTo(204);
        assertThat(setCookie(signedOut)).contains("Max-Age=0");
        assertThat(kindFor(accountCookie)).isEqualTo("ANONYMOUS");

        var wrongPassword = other.mutate("/api/session/login", Map.of("email", email, "password", "wrong"));
        var missingAccount = other.mutate("/api/session/login",
                Map.of("email", "missing-" + email, "password", "wrong"));
        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(missingAccount.statusCode()).isEqualTo(401);
        assertThat(read(wrongPassword).path("detail")).isEqualTo(read(missingAccount).path("detail"));
        assertThat(ownerOf(other)).isEqualTo(otherOwner);

        String otherGuestCookie = other.cookie;
        var signedIn = other.mutate("/api/session/login", Map.of("email", email, "password", PASSWORD));
        assertThat(signedIn.statusCode()).isEqualTo(200);
        assertThat(ownerOf(other)).isEqualTo(guestOwner);
        assertThat(other.cookie).isNotEqualTo(otherGuestCookie);
        assertThat(completions(guestOwner)).containsExactly("arr-1");
        assertThat(completions(otherOwner)).containsExactly("arr-2");
    }

    @Test
    void rejectsInvalidCredentialsWithoutIncludingSubmittedValues() throws Exception {
        Browser browser = createGuest();
        String tooLong = "é".repeat(37);
        var invalid = browser.mutate("/api/session/register", Map.of("email", "not-an-email", "password", tooLong));

        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(read(invalid).path("errors").path("email").isArray()).isTrue();
        assertThat(read(invalid).path("errors").path("password").get(0).asText()).contains("72 UTF-8 bytes");
        assertThat(invalid.body()).doesNotContain(tooLong, "not-an-email");
        assertThat(kindFor(browser.cookie)).isEqualTo("GUEST");
    }

    @Test
    void upgradesOneGuestOnlyOnceWhenRequestsOverlap() throws Exception {
        Browser guest = createGuest();
        UUID owner = ownerOf(guest);
        JsonNode csrf = guest.csrf();
        String firstEmail = UUID.randomUUID() + "@example.net";
        String secondEmail = UUID.randomUUID() + "@example.net";
        var first = http.sendAsync(guest.post("/api/session/register",
                Map.of("email", firstEmail, "password", PASSWORD), csrf), HttpResponse.BodyHandlers.ofString());
        var second = http.sendAsync(guest.post("/api/session/register",
                Map.of("email", secondEmail, "password", PASSWORD), csrf), HttpResponse.BodyHandlers.ofString());

        var firstResponse = first.get(20, TimeUnit.SECONDS);
        var secondResponse = second.get(20, TimeUnit.SECONDS);
        assertThat(List.of(firstResponse.statusCode(), secondResponse.statusCode()))
                .contains(201).hasSize(2).doesNotHaveDuplicates();
        var rejected = firstResponse.statusCode() == 201 ? secondResponse : firstResponse;
        assertThat(rejected.statusCode()).isIn(403, 409);
        String savedEmail = database.queryForObject(
                "SELECT normalized_email FROM tracker_owners WHERE id = ?", String.class, owner);
        String chosenEmail = firstResponse.statusCode() == 201 ? firstEmail : secondEmail;
        assertThat(savedEmail).isEqualTo(chosenEmail);
        assertThat(kindFor(guest.cookie)).isEqualTo("ANONYMOUS");
    }

    private Browser createGuest() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.mutate("/api/session/guest", null).statusCode()).isEqualTo(201);
        return browser;
    }

    private JsonNode read(HttpResponse<String> response) throws Exception {
        return json.readTree(response.body());
    }

    private String kindFor(String cookie) throws Exception {
        Browser browser = new Browser();
        browser.cookie = cookie;
        return read(browser.get("/api/session")).path("kind").asText();
    }

    private static String setCookie(HttpResponse<?> response) {
        return response.headers().firstValue("Set-Cookie").orElseThrow();
    }

    private UUID ownerOf(Browser browser) {
        String principal = database.queryForObject(
                "SELECT principal_name FROM spring_session WHERE session_id = ?", String.class, sessionId(browser));
        return UUID.fromString(principal);
    }

    private Integer idleTimeout(Browser browser) {
        return database.queryForObject("SELECT max_inactive_interval FROM spring_session WHERE session_id = ?",
                Integer.class, sessionId(browser));
    }

    private static String sessionId(Browser browser) {
        byte[] decoded = Base64.getDecoder().decode(browser.cookie.substring(COOKIE_NAME.length()));
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private List<String> completions(UUID owner) {
        return database.queryForList(
                "SELECT problem_id FROM problem_completions WHERE owner_id = ?", String.class, owner);
    }

    private class Browser {
        private String cookie;

        HttpResponse<String> get(String path) throws Exception {
            return send(request(path).GET().build());
        }

        JsonNode csrf() throws Exception {
            var response = get("/api/session/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            return read(response);
        }

        HttpResponse<String> mutate(String path, Map<String, String> body) throws Exception {
            return send(post(path, body, csrf()));
        }

        HttpRequest post(String path, Map<String, String> body, JsonNode csrf) throws Exception {
            var request = request(path);
            if (csrf != null) {
                request.header(csrf.path("headerName").asText(), csrf.path("token").asText());
            }
            if (body == null) {
                return request.POST(HttpRequest.BodyPublishers.noBody()).build();
            }
            return request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        }

        HttpResponse<String> send(HttpRequest request) throws Exception {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            response.headers().allValues("Set-Cookie").stream().filter(header -> header.startsWith(COOKIE_NAME))
                    .forEach(header -> cookie = header.contains("Max-Age=0") ? null : header.split(";", 2)[0]);
            return response;
        }

        private HttpRequest.Builder request(String path) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            return request;
        }
    }
}
