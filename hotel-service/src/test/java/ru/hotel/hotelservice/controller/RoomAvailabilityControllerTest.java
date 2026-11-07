package ru.hotel.hotelservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.hotelapi.model.ReleaseRequest;
import ru.hotel.hotelapi.model.ReservationResponse;
import ru.hotel.hotelapi.model.ReservationStatus;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.security.SecurityConfig;
import ru.hotel.hotelservice.service.RoomAvailabilityService;

@WebMvcTest(RoomAvailabilityController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "INTERNAL")
class RoomAvailabilityControllerTest {

    private static final String CONFIRM_BODY = """
            {"requestId": "req-1", "bookingId": 100, "startDate": "2027-01-10", "endDate": "2027-01-15"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomAvailabilityService availabilityService;

    @Test
    void confirmReturnsActiveReservation() throws Exception {
        // given
        given(availabilityService.confirm(eq(5L), any()))
                .willReturn(new ReservationResponse(
                        1L,
                        5L,
                        100L,
                        "req-1",
                        LocalDate.of(2027, 1, 10),
                        LocalDate.of(2027, 1, 15),
                        ReservationStatus.ACTIVE));

        // when / then
        mockMvc.perform(post("/api/rooms/5/confirm-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.requestId").value("req-1"));
    }

    @Test
    void confirmReturns409WhenSlotIsTaken() throws Exception {
        // given
        given(availabilityService.confirm(eq(5L), any())).willThrow(new ConflictException("Room 5 is not available"));

        // when / then
        mockMvc.perform(post("/api/rooms/5/confirm-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmReturns400WithoutRequestId() throws Exception {
        // given
        String body = """
                {"bookingId": 100, "startDate": "2027-01-10", "endDate": "2027-01-15"}
                """;

        // when / then
        mockMvc.perform(post("/api/rooms/5/confirm-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requestId")));
    }

    @Test
    void releaseReturns204() throws Exception {
        // when / then
        mockMvc.perform(post("/api/rooms/5/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId": "req-1"}
                                """))
                .andExpect(status().isNoContent());
        verify(availabilityService).release(5L, new ReleaseRequest("req-1"));
    }
}
