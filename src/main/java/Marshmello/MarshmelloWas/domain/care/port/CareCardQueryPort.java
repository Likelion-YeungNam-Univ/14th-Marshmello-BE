package Marshmello.MarshmelloWas.domain.care.port;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardView;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CareCardQueryPort {

    Optional<CareCardView> findByCheckInId(long checkInId);

    List<CareCardView> findByCheckInIds(Collection<Long> checkInIds);
}
