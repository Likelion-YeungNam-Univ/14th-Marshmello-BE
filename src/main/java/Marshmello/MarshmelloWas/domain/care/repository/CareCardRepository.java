package Marshmello.MarshmelloWas.domain.care.repository;

import Marshmello.MarshmelloWas.domain.care.entity.CareCard;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CareCardRepository extends JpaRepository<CareCard, Long> {

    Optional<CareCard> findByCheckInId(Long checkInId);

    List<CareCard> findByCheckInIdIn(Collection<Long> checkInIds);

    @Query("""
            select careCard
            from CareCard careCard
            join CheckIn checkIn on careCard.checkInId = checkIn.checkInId
            join fetch careCard.action
            where checkIn.userId = :userId
            order by careCard.createdDate desc, careCard.careCardId desc
            """)
    List<CareCard> findLatestByUserId(@Param("userId") long userId, Pageable pageable);
}
