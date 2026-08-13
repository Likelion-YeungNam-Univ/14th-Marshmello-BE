package Marshmello.MarshmelloWas.domain.care.controller;

import Marshmello.MarshmelloWas.domain.care.dto.CareCardCreationResult;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardFeedbackRequest;
import Marshmello.MarshmelloWas.domain.care.dto.CareCardResponse;
import Marshmello.MarshmelloWas.domain.care.service.CareCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class CareCardController {

    private final CareCardService careCardService;

    public CareCardController(CareCardService careCardService) {
        this.careCardService = careCardService;
    }

    @PostMapping("/check-ins/{checkInId}/care-card")
    public ResponseEntity<CareCardResponse> create(
            @PathVariable @Positive long checkInId
    ) {
        CareCardCreationResult result = careCardService.create(checkInId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.careCard());
    }

    @GetMapping("/check-ins/{checkInId}/care-card")
    public CareCardResponse getByCheckInId(
            @PathVariable @Positive long checkInId
    ) {
        return careCardService.getByCheckInId(checkInId);
    }

    @PatchMapping("/care-cards/{careCardId}/feedback")
    public ResponseEntity<Void> updateFeedback(
            @PathVariable @Positive long careCardId,
            @Valid @RequestBody CareCardFeedbackRequest request
    ) {
        careCardService.updateFeedback(careCardId, request.helpfulnessScore());
        return ResponseEntity.noContent().build();
    }
}
