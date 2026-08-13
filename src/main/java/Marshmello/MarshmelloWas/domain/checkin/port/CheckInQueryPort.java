package Marshmello.MarshmelloWas.domain.checkin.port;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInPageRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInSummaryView;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInTrendView;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInView;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.model.CheckInPage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInQueryPort {

    Optional<CheckInView> findOwnedById(long checkInId, long userId);

    CheckInPage<CheckInSummaryView> findPageByUserId(long userId, CheckInPageRequest pageRequest);

    List<CheckInTrendView> findTrendsByUserIdBetween(long userId, LocalDate periodStart, LocalDate periodEnd);
}
