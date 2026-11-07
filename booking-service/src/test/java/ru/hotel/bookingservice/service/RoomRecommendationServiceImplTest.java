package ru.hotel.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hotel.bookingapi.model.RecommendedRoom;
import ru.hotel.bookingservice.exception.BadRequestException;
import ru.hotel.bookingservice.exception.ServiceUnavailableException;

@ExtendWith(MockitoExtension.class)
class RoomRecommendationServiceImplTest {

    private static final Instant NOW = Instant.parse("2027-01-01T10:00:00Z");
    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);

    @Mock
    private HotelAvailability hotel;

    private RoomRecommendationServiceImpl recommendations;

    @BeforeEach
    void setUp() {
        recommendations = new RoomRecommendationServiceImpl(hotel, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void roomsComeLeastBookedFirstThenById() {
        // given
        given(hotel.recommendRooms(1L, START, END))
                .willReturn(List.of(
                        new RoomCandidate(7L, 1L, "202", 1),
                        new RoomCandidate(8L, 1L, "203", 0),
                        new RoomCandidate(3L, 1L, "103", 0)));

        // when
        List<RecommendedRoom> rooms = recommendations.recommend(1L, START, END);

        // then
        assertThat(rooms).extracting(RecommendedRoom::getId).containsExactly(3L, 8L, 7L);
        assertThat(rooms.getFirst()).isEqualTo(new RecommendedRoom(3L, 1L, "103", 0));
    }

    @Test
    void datesAreValidatedBeforeHotelServiceIsAsked() {
        // when / then
        assertThatThrownBy(() -> recommendations.recommend(null, END, START))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate must be before endDate");
        verifyNoInteractions(hotel);
    }

    @Test
    void unavailableHotelServiceIsReportedAs503() {
        // given
        given(hotel.recommendRooms(null, START, END)).willThrow(new IllegalStateException("circuit open"));

        // when / then
        assertThatThrownBy(() -> recommendations.recommend(null, START, END))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
