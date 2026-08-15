package Marshmello.MarshmelloWas.domain.checkin.controller;

import Marshmello.MarshmelloWas.domain.checkin.dto.ImageAnalysisResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.ImageAnalysisService;
import Marshmello.MarshmelloWas.global.exception.ApiException;
import Marshmello.MarshmelloWas.global.exception.ErrorCode;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/check-ins/images")
public class CheckInImageController {

    private final ImageAnalysisService imageAnalysisService;

    public CheckInImageController(ImageAnalysisService imageAnalysisService) {
        this.imageAnalysisService = imageAnalysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageAnalysisResponse analyze(@RequestPart("image") MultipartFile image) {
        try {
            return imageAnalysisService.analyze(image.getBytes(), image.getContentType());
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.IMAGE_ANALYSIS_FAILED);
        }
    }
}
