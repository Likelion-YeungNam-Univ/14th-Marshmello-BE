package Marshmello.MarshmelloWas.domain.checkin.repository;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByCheckInIdAndUserId(Long checkInId, Long userId);

    Page<CheckIn> findByUserId(Long userId, Pageable pageable);

    List<CheckIn> findByUserIdAndCheckInDateBetweenOrderByCheckInDateAscCheckInIdAsc(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
