package Marshmello.MarshmelloWas.domain.checkin.repository;

import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInTimelineItemResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findByCheckInIdAndUserId(Long checkInId, Long userId);

    boolean existsByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);

    Page<CheckIn> findByUserId(Long userId, Pageable pageable);

    @Query(
            value = """
                    select new Marshmello.MarshmelloWas.domain.checkin.dto.CheckInTimelineItemResponse(
                        checkIn.checkInId,
                        image.imageId,
                        checkIn.checkInDate,
                        checkIn.achieved,
                        checkIn.emotion
                    )
                    from CheckIn checkIn
                    join checkIn.image image
                    where checkIn.userId = :userId
                    order by checkIn.checkInDate desc, checkIn.checkInId desc
                    """,
            countQuery = """
                    select count(checkIn)
                    from CheckIn checkIn
                    where checkIn.userId = :userId
                      and checkIn.image is not null
                    """)
    Page<CheckInTimelineItemResponse> findTimelineByUserId(Long userId, Pageable pageable);

    List<CheckIn> findByUserIdAndCheckInDateBetweenOrderByCheckInDateAscCheckInIdAsc(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
