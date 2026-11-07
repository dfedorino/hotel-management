package ru.hotel.hotelservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.hotelservice.dto.RoomRequest;
import ru.hotel.hotelservice.dto.RoomResponse;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.service.RoomService;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    private static final String VALID_BODY = """
            {"hotelId": 1, "number": "101", "available": true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @Test
    void createReturns201WithCreatedRoom() throws Exception {
        // given
        given(roomService.create(any())).willReturn(new RoomResponse(5L, 1L, "101", true, 0));

        // when / then
        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.hotelId").value(1))
                .andExpect(jsonPath("$.timesBooked").value(0));
    }

    @Test
    void createReturns400WhenHotelIdIsMissing() throws Exception {
        // given
        String body = """
                {"number": "101"}
                """;

        // when / then
        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("hotelId")));
    }

    @Test
    void createReturns409WhenRoomNumberIsTaken() throws Exception {
        // given
        given(roomService.create(any())).willThrow(new ConflictException("Room 101 already exists in hotel 1"));

        // when / then
        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Room 101 already exists in hotel 1"));
    }

    @Test
    void getReturns404WhenRoomIsMissing() throws Exception {
        // given
        given(roomService.findById(42L)).willThrow(new NotFoundException("Room 42 not found"));

        // when / then
        mockMvc.perform(get("/api/rooms/42")).andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsUpdatedRoom() throws Exception {
        // given
        RoomRequest request = new RoomRequest(1L, "101", false);
        given(roomService.update(5L, request)).willReturn(new RoomResponse(5L, 1L, "101", false, 3));

        // when / then
        mockMvc.perform(put("/api/rooms/5").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hotelId": 1, "number": "101", "available": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void deleteReturns204() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/rooms/5")).andExpect(status().isNoContent());
        verify(roomService).delete(5L);
    }
}
