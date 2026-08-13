package Marshmello.MarshmelloWas.domain.care.dto;

import Marshmello.MarshmelloWas.domain.care.entity.CareCard;

public record CareCardGenerationResult(CareCardView careCard, boolean created) {
}
