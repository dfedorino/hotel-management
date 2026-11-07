package ru.hotel.hotelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelapi.model.HotelRequest;
import ru.hotel.hotelapi.model.HotelResponse;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.mapper.HotelMapper;
import ru.hotel.hotelservice.repository.HotelRepository;
import ru.hotel.hotelservice.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    private HotelServiceImpl hotelService;

    @BeforeEach
    void setUp() {
        hotelService = new HotelServiceImpl(hotelRepository, roomRepository, Mappers.getMapper(HotelMapper.class));
    }

    @Test
    void createSavesHotelAndReturnsResponse() {
        // given
        given(hotelRepository.save(any(Hotel.class))).willAnswer(invocation -> {
            Hotel hotel = invocation.getArgument(0);
            hotel.setId(7L);
            return hotel;
        });

        // when
        HotelResponse response = hotelService.create(new HotelRequest("Grand", "Main st. 1"));

        // then
        assertThat(response).isEqualTo(new HotelResponse(7L, "Grand", "Main st. 1"));
    }

    @Test
    void updateChangesExistingHotel() {
        // given
        Hotel hotel = new Hotel("Old", "Old st.");
        hotel.setId(1L);
        given(hotelRepository.findById(1L)).willReturn(Optional.of(hotel));

        // when
        HotelResponse response = hotelService.update(1L, new HotelRequest("New", "New st."));

        // then
        assertThat(response).isEqualTo(new HotelResponse(1L, "New", "New st."));
    }

    @Test
    void findByIdThrowsNotFoundForMissingHotel() {
        // given
        given(hotelRepository.findById(42L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> hotelService.findById(42L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Hotel 42 not found");
    }

    @Test
    void deleteThrowsNotFoundForMissingHotel() {
        // given
        given(hotelRepository.existsById(42L)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> hotelService.delete(42L)).isInstanceOf(NotFoundException.class);
        verify(hotelRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrowsConflictWhenHotelHasRooms() {
        // given
        given(hotelRepository.existsById(1L)).willReturn(true);
        given(roomRepository.existsByHotelId(1L)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> hotelService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Hotel 1 has rooms and cannot be deleted");
        verify(hotelRepository, never()).deleteById(any());
    }
}
