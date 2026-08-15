package Marshmello.MarshmelloWas.domain.checkin.controller;

import Marshmello.MarshmelloWas.domain.checkin.dto.ImageAnalysisResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.ImageUrlResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInQueryService;
import Marshmello.MarshmelloWas.domain.checkin.service.ImageAnalysisService;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/check-ins/images")
public class CheckInImageController {

    private final ImageAnalysisService imageAnalysisService;
    private final CheckInQueryService checkInQueryService;

    public CheckInImageController(
            ImageAnalysisService imageAnalysisService,
            CheckInQueryService checkInQueryService
    ) {
        this.imageAnalysisService = imageAnalysisService;
        this.checkInQueryService = checkInQueryService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageAnalysisResponse analyze(@RequestPart("image") MultipartFile image) {
        try {
            return imageAnalysisService.analyze(image.getBytes(), image.getContentType());
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.IMAGE_ANALYSIS_FAILED);
        }
    }

    @GetMapping("/{imageId}/url")
    public ImageUrlResponse createImageUrl(@PathVariable @Positive long imageId) {
        return checkInQueryService.createImageUrl(imageId);
    }
}
