package Marshmello.MarshmelloWas.domain.care.adapter;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardGenerationRequest;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGeneratedText;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGenerationException;
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
