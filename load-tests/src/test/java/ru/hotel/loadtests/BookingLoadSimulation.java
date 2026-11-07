package ru.hotel.loadtests;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Many guests book the same dates at once with automatic room selection, through the gateway on demo data.
 *
 * <ul>
 *   <li>every booking either gets a room (201) or is refused because all rooms are gone (409): no 5xx, no other error;
 *   <li>afterwards the occupancy report shows no room booked twice for the period (no overbooking) and every room in
 *       service taken (no idle room while guests were refused).
 * </ul>
 *
 * <p>Run against the started system: {@code ./mvnw -Pload-tests -pl load-tests gatling:test}. System properties:
 * {@code baseUrl} (http://localhost:8080), {@code users} (40), {@code rampSeconds} (10), {@code adminPassword}.
 */
public class BookingLoadSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int USERS = Integer.getInteger("users", 40);
    private static final int RAMP_SECONDS = Integer.getInteger("rampSeconds", 10);
    private static final String ADMIN_PASSWORD = System.getProperty("adminPassword", "demo-password");

    /** Far from the demo reservations, so all rooms in service are free at the start. */
    private static final LocalDate START = LocalDate.now().plusDays(60);

    private static final LocalDate END = START.plusDays(3);
    private static final long NIGHTS = ChronoUnit.DAYS.between(START, END);

    /** Demo data has exactly one room out of service (Grand Plaza, 105). */
    private static final int ROOMS_OUT_OF_SERVICE = 1;

    private final HttpProtocolBuilder protocol =
            http.baseUrl(BASE_URL).contentTypeHeader("application/json").acceptHeader("application/json");

    private final ChainBuilder registerGuest = exec(session -> session.set("username", "guest-" + shortId()))
            .exec(http("register")
                    .post("/api/bookings/users/register")
                    .body(StringBody("{\"username\": \"#{username}\", \"password\": \"load-test-pass\"}"))
                    .check(status().is(201))
                    .check(jmesPath("accessToken").saveAs("token")));

    private final ChainBuilder lookAtRecommendations = exec(http("recommendations")
            .get("/api/bookings/rooms/recommend")
            .queryParam("startDate", START.toString())
            .queryParam("endDate", END.toString())
            .header("Authorization", "Bearer #{token}")
            .check(status().is(200)));

    private final ChainBuilder bookAnyRoom = exec(session ->
                    session.set("requestId", UUID.randomUUID().toString()))
            .exec(http("book with autoSelect")
                    .post("/api/bookings")
                    .header("Authorization", "Bearer #{token}")
                    .body(StringBody("{\"autoSelect\": true, \"startDate\": \"" + START + "\", \"endDate\": \"" + END
                            + "\", \"requestId\": \"#{requestId}\"}"))
                    .check(status().in(201, 409)));

    private final ChainBuilder lookAtHistory = exec(http("history")
            .get("/api/bookings")
            .header("Authorization", "Bearer #{token}")
            .check(status().is(200)));

    private final ScenarioBuilder guests = scenario("guests book the same dates")
            .exec(registerGuest, lookAtRecommendations, bookAnyRoom, lookAtHistory);

    private final ScenarioBuilder occupancyCheck = scenario("occupancy after the rush")
            .exec(http("admin login")
                    .post("/api/bookings/users/auth")
                    .body(StringBody("{\"username\": \"admin\", \"password\": \"" + ADMIN_PASSWORD + "\"}"))
                    .check(status().is(200))
                    .check(jmesPath("accessToken").saveAs("adminToken")))
            .exec(http("occupancy")
                    .get("/api/rooms/occupancy")
                    .queryParam("startDate", START.toString())
                    .queryParam("endDate", END.toString())
                    .header("Authorization", "Bearer #{adminToken}")
                    .check(status().is(200))
                    .check(jmesPath("length(rooms[?bookedNights > `" + NIGHTS + "`])")
                            .ofInt()
                            .is(0)
                            .name("no room booked twice"))
                    .check(jmesPath("length(rooms[?bookedNights == `0`])")
                            .ofInt()
                            .is(ROOMS_OUT_OF_SERVICE)
                            .name("only rooms out of service stay idle")));

    public BookingLoadSimulation() {
        setUp(guests.injectOpen(rampUsers(USERS).during(Duration.ofSeconds(RAMP_SECONDS)))
                        .andThen(occupancyCheck.injectOpen(atOnceUsers(1))))
                .protocols(protocol)
                .assertions(
                        global().failedRequests().count().is(0L),
                        global().responseTime().percentile(95.0).lt(1000));
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
