package com.airline.repository;

import com.airline.domain.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    Optional<Passenger> findByPublicId(UUID publicId);

    Optional<Passenger> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPassportNumber(String passportNumber);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPassportNumberAndIdNot(String passportNumber, Long id);

    List<Passenger> findAllByOrderByLastNameAscFirstNameAsc();
}
