package com.solvestack.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class ApiBrowser {
    static final String COOKIE_NAME = "SOLVESTACK_SESSION=";

    private final HttpClient http;
    private final ObjectMapper json;
    private final URI origin;
    String cookie;

    ApiBrowser(HttpClient http, ObjectMapper json, int port) {
        this.http = http;
        this.json = json;
        this.origin = URI.create("http://127.0.0.1:" + port);
    }

    HttpResponse<String> get(String path) throws Exception {
        return send(request("GET", path, null, null));
    }

    JsonNode csrf() throws Exception {
        var response = get("/api/session/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return json.readTree(response.body());
    }

    HttpResponse<String> mutate(String path, Map<String, String> body) throws Exception {
        return send(post(path, body, csrf()));
    }

    HttpResponse<String> write(String method, String path) throws Exception {
        return send(request(method, path, null, csrf()));
    }

    HttpRequest post(String path, Map<String, String> body, JsonNode csrf) throws Exception {
        return request("POST", path, body, csrf);
    }

    HttpRequest request(String method, String path, Map<String, String> body, JsonNode csrf) throws Exception {
        var request = HttpRequest.newBuilder(origin.resolve(path)).timeout(Duration.ofSeconds(15));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        if (csrf != null) {
            request.header(csrf.path("headerName").asText(), csrf.path("token").asText());
        }
        if (body == null) {
            return request.method(method, HttpRequest.BodyPublishers.noBody()).build();
        }
        return request.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
    }

    HttpResponse<String> send(HttpRequest request) throws Exception {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        response.headers().allValues("Set-Cookie").stream().filter(header -> header.startsWith(COOKIE_NAME))
                .forEach(header -> cookie = header.contains("Max-Age=0") ? null : header.split(";", 2)[0]);
        return response;
    }

    // Concurrent tests keep the same session and inspect responses without racing the cookie jar.
    CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
