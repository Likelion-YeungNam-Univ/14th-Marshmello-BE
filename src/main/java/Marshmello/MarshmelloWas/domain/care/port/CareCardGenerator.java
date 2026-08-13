package Marshmello.MarshmelloWas.domain.care.port;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import Marshmello.MarshmelloWas.domain.care.model.CareCardGeneratedText;

public interface CareCardGenerator {

    CareCardGeneratedText generate(CareCardGenerationRequest request);
}
