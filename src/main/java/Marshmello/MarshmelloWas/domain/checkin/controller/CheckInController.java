package Marshmello.MarshmelloWas.domain.checkin.controller;

import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInCreateRequest;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInEmotionResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.CheckInResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MonthlyCheckInCountResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.MostFrequentBodyRegionResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInQueryService;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@Validated
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

    @PostMapping(params = "date")
    public ResponseEntity<CheckInResponse> createForDate(
            @Valid @RequestBody CheckInCreateRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkInService.create(request, date));
    }

    @GetMapping
    public List<CheckInResponse> getByDate(
            @RequestParam LocalDate date
    ) {
        return checkInQueryService.getByDate(date);
    }

    @GetMapping("/emotions")
    public List<CheckInEmotionResponse> getEmotionsByMonth(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return checkInQueryService.getEmotionsByMonth(month);
    }

    @GetMapping("/count")
    public MonthlyCheckInCountResponse getMonthlyCount(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        MonthlyCheckInCountResponse response = checkInQueryService.getMonthlyCount(month);
        return new MonthlyCheckInCountResponse(month, response.count(), response.achievedCount());
    }

    @GetMapping("/body-diaries/top-region")
    public MostFrequentBodyRegionResponse getMostFrequentBodyRegion(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return checkInQueryService.getMostFrequentBodyRegion(month);
    }

    @DeleteMapping("/{checkInId}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive long checkInId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        checkInService.delete(checkInId, date);
        return ResponseEntity.noContent().build();
    }
}
