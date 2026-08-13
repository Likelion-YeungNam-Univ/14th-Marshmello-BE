package Marshmello.MarshmelloWas.infrastructure.ai.openai;

import Marshmello.MarshmelloWas.domain.care.model.CareCardGeneratedText;
import Marshmello.MarshmelloWas.domain.care.model.CareCardGenerationException;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import Marshmello.MarshmelloWas.domain.care.service.port.CareCardGenerator;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

@Component
@Fallback
public class UnavailableCareCardGenerator implements CareCardGenerator {

    @Override
    public CareCardGeneratedText generate(CareCardGenerationRequest request) {
        throw new CareCardGenerationException(CareCardGenerationException.Reason.UNAVAILABLE);
    }
}
