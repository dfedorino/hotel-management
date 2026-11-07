package ru.hotel.bookingservice.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.time.Duration;

/**
 * Request patterns for the Hotel Service stub, scoped to one saga request. Tests share the stub server, and a request
 * of an earlier test that waited on a delayed answer can be journaled late, so counts are taken per requestId.
 */
public final class HotelServiceStubs {

    private static final Duration PATIENCE = Duration.ofSeconds(2);
    private static final long PAUSE_MILLIS = 50;

    private HotelServiceStubs() {}

    public static RequestPatternBuilder confirmsOf(Long roomId, String requestId) {
        return postRequestedFor(urlPathEqualTo("/api/rooms/" + roomId + "/confirm-availability"))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo(requestId)));
    }

    public static RequestPatternBuilder releasesOf(Long roomId, String requestId) {
        return postRequestedFor(urlPathEqualTo("/api/rooms/" + roomId + "/release"))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo(requestId)));
    }

    /** Retries an assertion until a delayed stub has finished answering (at most {@link #PATIENCE}). */
    public static void eventually(Runnable assertion) {
        long deadline = System.nanoTime() + PATIENCE.toNanos();
        while (true) {
            try {
                assertion.run();
                return;
            } catch (AssertionError notYet) {
                if (System.nanoTime() > deadline) {
                    throw notYet;
                }
                pause();
            }
        }
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
