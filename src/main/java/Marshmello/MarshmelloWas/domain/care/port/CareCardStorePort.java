package Marshmello.MarshmelloWas.domain.care.port;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationResult;
import Marshmello.MarshmelloWas.domain.care.entity.Action;
import Marshmello.MarshmelloWas.domain.care.model.ActionCandidate;
import Marshmello.MarshmelloWas.domain.care.model.CareCardGeneratedText;

public interface CareCardStorePort {

    CareCardGenerationResult save(long checkInId, ActionCandidate action, CareCardGeneratedText generatedText);
}
