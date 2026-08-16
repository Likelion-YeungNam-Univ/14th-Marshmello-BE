package Marshmello.MarshmelloWas.domain.checkin.repository;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInEmotionResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByCheckInIdAndUserId(Long checkInId, Long userId);

    boolean existsByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);

    long countByUserIdAndCheckInDateGreaterThanEqualAndCheckInDateLessThan(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    Page<CheckIn> findByUserId(Long userId, Pageable pageable);

    @Query("""
            select new Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryResponse(
                checkIn.checkInId,
                image.imageId,
                checkIn.checkInDate,
                checkIn.achieved,
                checkIn.emotion
            )
            from CheckIn checkIn
            join checkIn.image image
            where checkIn.userId = :userId
              and checkIn.checkInDate = :date
            """)
    List<CheckInSummaryResponse> findSummariesByUserIdAndDate(Long userId, LocalDate date);

    @Query("""
            select new Marshmello.MarshmelloWas.domain.checkin.dto.CheckInEmotionResponse(
                checkIn.checkInDate,
                checkIn.emotion
            )
            from CheckIn checkIn
            where checkIn.userId = :userId
              and checkIn.checkInDate >= :periodStart
              and checkIn.checkInDate < :periodEnd
            order by checkIn.checkInDate asc, checkIn.checkInId asc
            """)
    List<CheckInEmotionResponse> findEmotionsByUserIdAndPeriod(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    List<CheckIn> findByUserIdAndCheckInDateBetweenOrderByCheckInDateAscCheckInIdAsc(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
