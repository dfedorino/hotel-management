package ru.hotel.bookingservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.bookingservice.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("PMD.SignatureDeclareThrowsException") // MockMvc.perform() declares Exception
class AuthAndUsersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BookingRepository bookingRepository;

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminListsAndViewsUsers() throws Exception {
        // given
        User root = userRepository.save(new User("root", passwordEncoder.encode("root-pass-1"), Role.ADMIN));
        User bob = userRepository.save(new User("bob", "hash", Role.USER));
        String adminToken = login("root", "root-pass-1");

        // when / then
        mockMvc.perform(get("/api/bookings/users")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value(root.getUsername()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        mockMvc.perform(get("/api/bookings/users/" + bob.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void userCannotListUsers() throws Exception {
        // given
        String userToken = register("alice");

        // when / then
        mockMvc.perform(get("/api/bookings/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void userWithBookingsCannotBeDeleted() throws Exception {
        // given
        userRepository.save(new User("root", passwordEncoder.encode("root-pass-1"), Role.ADMIN));
        String adminToken = login("root", "root-pass-1");
        User guest = userRepository.save(new User("bob", "hash", Role.USER));
        bookingRepository.save(new Booking(
                guest.getId(), 5L, LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15), "req-1", Instant.now()));

        // when / then
        mockMvc.perform(delete("/api/bookings/users/" + guest.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message").value("User " + guest.getId() + " has bookings and cannot be deleted"));
    }

    @Test
    void registeredUserGetsBearerTokenValidForOneHour() throws Exception {
        // when / then
        mockMvc.perform(post("/api/bookings/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "secret-pass")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void registeringTakenUsernameGets409() throws Exception {
        // given
        register("alice");

        // when / then
        mockMvc.perform(post("/api/bookings/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "other-pass")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registrationValidatesCredentials() throws Exception {
        // when / then
        mockMvc.perform(post("/api/bookings/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("al", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password")));
    }

    @Test
    void wrongPasswordGets401WithErrorBody() throws Exception {
        // given
        register("alice");

        // when / then
        mockMvc.perform(post("/api/bookings/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "wrong-pass")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.path").value("/api/bookings/users/auth"));
    }

    @Test
    void userCannotAdministerUsers() throws Exception {
        // given
        String userToken = register("alice");

        // when / then
        mockMvc.perform(post("/api/bookings/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser("bob", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesUpdatesAndDeletesUser() throws Exception {
        // given
        userRepository.save(new User("root", passwordEncoder.encode("root-pass-1"), Role.ADMIN));
        String adminToken = login("root", "root-pass-1");

        // when
        String created = mockMvc.perform(post("/api/bookings/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser("bob", "USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        // then
        mockMvc.perform(patch("/api/bookings/users/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.username").value("bob"));
        mockMvc.perform(delete("/api/bookings/users/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/bookings/users/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private String register(String username) throws Exception {
        String body = mockMvc.perform(post("/api/bookings/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret-pass")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/bookings/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String credentials(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }

    private static String newUser(String username, String role) {
        return """
                {"username": "%s", "password": "secret-pass", "role": "%s"}
                """.formatted(username, role);
    }
}
