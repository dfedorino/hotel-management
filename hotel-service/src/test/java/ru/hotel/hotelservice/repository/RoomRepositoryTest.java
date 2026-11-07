package ru.hotel.hotelservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.Room;

@DataJpaTest
class RoomRepositoryTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void savesRoomLinkedToHotelWithDefaultStatistics() {
        // given
        Hotel hotel = hotelRepository.save(new Hotel("Grand", "Main st. 1"));

        // when
        Room saved = roomRepository.save(new Room(hotel, "101"));

        // then
        Room found = roomRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getHotel().getId()).isEqualTo(hotel.getId());
        assertThat(found.getNumber()).isEqualTo("101");
        assertThat(found.isAvailable()).isTrue();
        assertThat(found.getTimesBooked()).isZero();
    }

    @Test
    void findsRoomsOfGivenHotelOnly() {
        // given
        Hotel grand = hotelRepository.save(new Hotel("Grand", "Main st. 1"));
        Hotel plaza = hotelRepository.save(new Hotel("Plaza", "Second st. 2"));
        roomRepository.save(new Room(grand, "101"));
        roomRepository.save(new Room(grand, "102"));
        roomRepository.save(new Room(plaza, "201"));

        // when
        List<Room> rooms = roomRepository.findByHotelId(grand.getId());

        // then
        assertThat(rooms).extracting(Room::getNumber).containsExactlyInAnyOrder("101", "102");
    }
}
