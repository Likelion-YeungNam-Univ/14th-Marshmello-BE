package Marshmello.MarshmelloWas.domain.checkin.repository;

import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiary;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.BodyDiaryId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BodyDiaryRepository extends JpaRepository<BodyDiary, BodyDiaryId> {

    List<BodyDiary> findByBodyDiaryIdCheckInIdInOrderByBodyDiaryIdBodyRegionAsc(Collection<Long> checkInIds);

    @Query("""
            select bodyDiary.bodyDiaryId.bodyRegion
            from BodyDiary bodyDiary
            where bodyDiary.checkIn.userId = :userId
              and bodyDiary.checkIn.checkInDate >= :periodStart
              and bodyDiary.checkIn.checkInDate < :periodEnd
            group by bodyDiary.bodyDiaryId.bodyRegion
            order by count(bodyDiary) desc,
                     bodyDiary.bodyDiaryId.bodyRegion asc
            """)
    List<Short> findBodyRegionsOrderedByCount(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
