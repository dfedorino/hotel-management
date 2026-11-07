package ru.hotel.hotelservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A slot of a room held for a booking over [startDate, endDate); endDate is the check-out day. */
@Entity
@Table(
        name = "room_reservations",
        indexes = @Index(name = "idx_reservations_room_dates", columnList = "room_id, start_date, end_date"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    public RoomReservation(Room room, Long bookingId, String requestId, LocalDate startDate, LocalDate endDate) {
        this.room = room;
        this.bookingId = bookingId;
        this.requestId = requestId;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
