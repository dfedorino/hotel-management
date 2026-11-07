package ru.hotel.hotelservice.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.hotelservice.dto.OccupancyQuery;
import ru.hotel.hotelservice.dto.OccupancyReport;
import ru.hotel.hotelservice.dto.RoomOccupancy;
import ru.hotel.hotelservice.security.SecurityConfig;
import ru.hotel.hotelservice.service.OccupancyService;

@WebMvcTest(OccupancyController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class OccupancyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OccupancyService occupancyService;

    @Test
    void returnsOccupancyReportForPeriod() throws Exception {
        // given
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 1, 11);
        given(occupancyService.report(new OccupancyQuery(1L, start, end))).willReturn(new OccupancyReport(
                start, end, 10, 1, 0.5, List.of(new RoomOccupancy(10L, 1L, "101", 2, 5, 0.5))));

        // when / then
        mockMvc.perform(get("/api/rooms/occupancy")
                        .param("hotelId", "1")
                        .param("startDate", "2027-01-01")
                        .param("endDate", "2027-01-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageOccupancy").value(0.5))
                .andExpect(jsonPath("$.rooms[0].bookedNights").value(5));
    }

    @Test
    void returns400WithoutPeriod() throws Exception {
        // when / then
        mockMvc.perform(get("/api/rooms/occupancy"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("startDate")));
    }

    @Test
    void returns400WhenStartIsNotBeforeEnd() throws Exception {
        // when / then
        mockMvc.perform(get("/api/rooms/occupancy")
                        .param("startDate", "2027-01-11")
                        .param("endDate", "2027-01-01"))
                .andExpect(status().isBadRequest());
    }
}
