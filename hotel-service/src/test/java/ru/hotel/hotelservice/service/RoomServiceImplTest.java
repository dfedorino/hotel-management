package ru.hotel.hotelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelapi.model.RoomRequest;
import ru.hotel.hotelapi.model.RoomResponse;
import ru.hotel.hotelservice.dto.RoomSearch;
import ru.hotel.hotelservice.exception.BadRequestException;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.mapper.RoomMapper;
import ru.hotel.hotelservice.repository.HotelRepository;
import ru.hotel.hotelservice.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    private RoomServiceImpl roomService;

    private Hotel hotel;

    @BeforeEach
    void setUp() {
        roomService = new RoomServiceImpl(roomRepository, hotelRepository, Mappers.getMapper(RoomMapper.class));
        hotel = new Hotel("Grand", "Main st. 1");
        hotel.setId(1L);
    }

    @Test
    void createSavesRoomInExistingHotel() {
        // given
        given(hotelRepository.findById(1L)).willReturn(Optional.of(hotel));
        given(roomRepository.existsByHotelIdAndNumber(1L, "101")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(5L);
            return room;
        });

        // when
        RoomResponse response = roomService.create(new RoomRequest(1L, "101"));

        // then
        assertThat(response).isEqualTo(new RoomResponse(5L, 1L, "101", true, 0));
    }

    @Test
    void createFailsWhenHotelIsMissing() {
        // given
        given(hotelRepository.findById(1L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> roomService.create(new RoomRequest(1L, "101").available(true)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Hotel 1 not found");
    }

    @Test
    void createFailsWhenRoomNumberIsTaken() {
        // given
        given(hotelRepository.findById(1L)).willReturn(Optional.of(hotel));
        given(roomRepository.existsByHotelIdAndNumber(1L, "101")).willReturn(true);

        // when / then
        assertThatThrownBy(() -> roomService.create(new RoomRequest(1L, "101").available(true)))
                .isInstanceOf(ConflictException.class);
        verify(roomRepository, never()).save(any());
    }

    @Test
    void updateChangesNumberAndAvailabilityButKeepsStatistics() {
        // given
        Room room = new Room(hotel, "101");
        room.setId(5L);
        room.setTimesBooked(3);
        given(roomRepository.findById(5L)).willReturn(Optional.of(room));
        given(hotelRepository.findById(1L)).willReturn(Optional.of(hotel));

        // when
        RoomResponse response = roomService.update(5L, new RoomRequest(1L, "102").available(false));

        // then
        assertThat(response).isEqualTo(new RoomResponse(5L, 1L, "102", false, 3));
    }

    @Test
    void findFreeWithDatesSortsById() {
        // given
        Room room = new Room(hotel, "101");
        given(roomRepository.findFree(1L, START, END, Sort.by("id"))).willReturn(List.of(room));

        // when
        List<RoomResponse> rooms = roomService.findFree(new RoomSearch(1L, START, END));

        // then
        assertThat(rooms).extracting(RoomResponse::getNumber).containsExactly("101");
    }

    @Test
    void findRecommendedSortsByTimesBookedThenId() {
        // given
        given(roomRepository.findFree(null, START, END, Sort.by("timesBooked", "id"))).willReturn(List.of());

        // when
        roomService.findRecommended(new RoomSearch(null, START, END));

        // then
        verify(roomRepository).findFree(null, START, END, Sort.by("timesBooked", "id"));
    }

    @Test
    void findFreeWithoutDatesReturnsRoomsInService() {
        // given
        given(roomRepository.findAvailable(null, Sort.by("id"))).willReturn(List.of());

        // when
        roomService.findFree(new RoomSearch(null, null, null));

        // then
        verify(roomRepository).findAvailable(null, Sort.by("id"));
    }

    @Test
    void searchRejectsStartNotBeforeEnd() {
        // when / then
        assertThatThrownBy(() -> roomService.findFree(new RoomSearch(null, END, START)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate must be before endDate");
        assertThatThrownBy(() -> roomService.findRecommended(new RoomSearch(null, START, START)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void searchRejectsOnlyOneDate() {
        // when / then
        assertThatThrownBy(() -> roomService.findFree(new RoomSearch(null, START, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate and endDate must be given together");
    }

    @Test
    void deleteThrowsNotFoundForMissingRoom() {
        // given
        given(roomRepository.existsById(42L)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> roomService.delete(42L)).isInstanceOf(NotFoundException.class);
    }
}
