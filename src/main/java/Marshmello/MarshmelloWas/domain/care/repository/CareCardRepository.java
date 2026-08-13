package Marshmello.MarshmelloWas.domain.care.repository;

import Marshmello.MarshmelloWas.domain.care.entity.CareCard;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareCardRepository extends JpaRepository<CareCard, Long> {

    Optional<CareCard> findByCheckInId(Long checkInId);

    List<CareCard> findByCheckInIdIn(Collection<Long> checkInIds);
}
