package Marshmello.MarshmelloWas.domain.checkin.controller;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCreateRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInTimelineItemResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInQueryService;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInService;
import Marshmello.MarshmelloWas.global.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/check-ins")
public class CheckInController {

    private final CheckInService checkInService;
    private final CheckInQueryService checkInQueryService;

    public CheckInController(CheckInService checkInService, CheckInQueryService checkInQueryService) {
        this.checkInService = checkInService;
        this.checkInQueryService = checkInQueryService;
    }

    @PostMapping
    public ResponseEntity<CheckInResponse> create(
            @Valid @RequestBody CheckInCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkInService.create(request));
    }

    @GetMapping
    public PageResponse<CheckInTimelineItemResponse> getTimeline(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return checkInQueryService.getTimeline(page, size);
    }
}
