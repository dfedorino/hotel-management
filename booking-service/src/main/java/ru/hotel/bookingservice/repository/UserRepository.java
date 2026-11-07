package ru.hotel.bookingservice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hotel.bookingservice.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
}
