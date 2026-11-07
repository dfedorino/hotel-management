package ru.hotel.bookingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stay of a user in a room over [startDate, endDate). {@code roomId} refers to Hotel Service (no foreign key across
 * services); {@code requestId} is the correlation id that makes repeated requests idempotent.
 */
@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    /**
     * Key of the slot in Hotel Service: the requestId for the first room tried, {@code requestId#n} for the n-th room
     * of an automatic selection. Confirm, compensation and cancellation all use it.
     */
    @Column(name = "slot_request_id", nullable = false, length = 70)
    private String slotRequestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Booking(
            Long userId, Long roomId, LocalDate startDate, LocalDate endDate, String requestId, Instant createdAt) {
        this.userId = userId;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requestId = requestId;
        this.slotRequestId = requestId;
        this.createdAt = createdAt;
    }

    /** Automatic selection only: a room taken meanwhile is replaced while the booking is still PENDING. */
    public void moveTo(Long newRoomId, String newSlotRequestId) {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking " + id + " is " + status + " and cannot change its room");
        }
        this.roomId = newRoomId;
        this.slotRequestId = newSlotRequestId;
    }

    /** PENDING to CONFIRMED once Hotel Service has locked the slot. */
    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking " + id + " is " + status + " and cannot be confirmed");
        }
        status = BookingStatus.CONFIRMED;
    }

    /**
     * Moves to CANCELLED from any state.
     *
     * @return false if the booking was already cancelled
     */
    public boolean cancel() {
        if (status == BookingStatus.CANCELLED) {
            return false;
        }
        status = BookingStatus.CANCELLED;
        return true;
    }
}
