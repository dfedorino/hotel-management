package ru.hotel.hotelservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.hotelapi.model.HotelRequest;
import ru.hotel.hotelapi.model.HotelResponse;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.security.SecurityConfig;
import ru.hotel.hotelservice.service.HotelService;

@WebMvcTest(HotelController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class HotelControllerTest {

    private static final String VALID_BODY = """
            {"name": "Grand", "address": "Main st. 1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HotelService hotelService;

    @Test
    void createReturns201WithCreatedHotel() throws Exception {
        // given
        given(hotelService.create(any())).willReturn(new HotelResponse(1L, "Grand", "Main st. 1"));

        // when / then
        mockMvc.perform(post("/api/hotels").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Grand"));
    }

    @Test
    void createReturns400WithErrorBodyWhenNameIsBlank() throws Exception {
        // given
        String body = """
                {"name": " ", "address": "Main st. 1"}
                """;

        // when / then
        mockMvc.perform(post("/api/hotels").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/hotels"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));
    }

    @Test
    void listReturnsAllHotels() throws Exception {
        // given
        given(hotelService.findAll()).willReturn(List.of(
                new HotelResponse(1L, "Grand", "Main st. 1"),
                new HotelResponse(2L, "Plaza", "Second st. 2")));

        // when / then
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("Plaza"));
    }

    @Test
    void getReturns404WithErrorBodyWhenHotelIsMissing() throws Exception {
        // given
        given(hotelService.findById(42L)).willThrow(new NotFoundException("Hotel 42 not found"));

        // when / then
        mockMvc.perform(get("/api/hotels/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hotel 42 not found"));
    }

    @Test
    void updateReturnsUpdatedHotel() throws Exception {
        // given
        given(hotelService.update(eq(1L), any())).willReturn(new HotelResponse(1L, "Grand", "Main st. 1"));

        // when / then
        mockMvc.perform(put("/api/hotels/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Main st. 1"));
        verify(hotelService).update(1L, new HotelRequest("Grand", "Main st. 1"));
    }

    @Test
    void deleteReturns204() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/hotels/1")).andExpect(status().isNoContent());
        verify(hotelService).delete(1L);
    }

    @Test
    void deleteReturns404WhenHotelIsMissing() throws Exception {
        // given
        willThrow(new NotFoundException("Hotel 42 not found")).given(hotelService).delete(42L);

        // when / then
        mockMvc.perform(delete("/api/hotels/42")).andExpect(status().isNotFound());
    }
}
