package Marshmello.MarshmelloWas.domain.care.port;

import Marshmello.MarshmelloWas.domain.care.model.CareCardGenerationContext;

import java.util.Optional;

public interface CareCardSourcePort {

    Optional<CareCardGenerationContext> findGenerationContext(long checkInId, long userId);
}
