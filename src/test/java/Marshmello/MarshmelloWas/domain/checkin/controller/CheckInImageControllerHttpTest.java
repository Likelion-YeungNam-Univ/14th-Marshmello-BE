package Marshmello.MarshmelloWas.domain.checkin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.checkin.dto.ImageAnalysisResponse;
import Marshmello.MarshmelloWas.domain.checkin.dto.ImageUrlResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.CheckInQueryService;
import Marshmello.MarshmelloWas.domain.checkin.service.ImageAnalysisService;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckInImageControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageAnalysisService imageAnalysisService;

    @MockitoBean
    private CheckInQueryService checkInQueryService;

    @Test
    void returnsFalseWhenDaveyScoreIsNotAvailable() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "body.png", "image/png", new byte[]{1, 2, 3});
        when(imageAnalysisService.analyze(any(byte[].class), eq("image/png")))
                .thenReturn(new ImageAnalysisResponse(false, null));

        mockMvc.perform(multipart("/api/check-ins/images/analyze")
                        .file(image)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detected").value(false))
                .andExpect(jsonPath("$.imageId").doesNotExist())
                .andExpect(jsonPath("$.score").doesNotExist());
    }

    @Test
    void returnsOnlyDetectionAndImageIdWhenAnalysisSucceeds() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "body.png", "image/png", new byte[]{1, 2, 3});
        when(imageAnalysisService.analyze(any(byte[].class), eq("image/png")))
                .thenReturn(new ImageAnalysisResponse(true, 21L));

        mockMvc.perform(multipart("/api/check-ins/images/analyze")
                        .file(image)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detected").value(true))
                .andExpect(jsonPath("$.imageId").value(21))
                .andExpect(jsonPath("$.score").doesNotExist());
    }

    @Test
    void requiresAuthentication() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "body.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/check-ins/images/analyze")
                        .file(image)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(imageAnalysisService);
    }

    @Test
    void returnsShortLivedOriginalImageUrl() throws Exception {
        when(checkInQueryService.createImageUrl(21L)).thenReturn(new ImageUrlResponse(
                21L,
                URI.create("https://bucket.example.test/image?signature=redacted"),
                Instant.parse("2026-08-15T04:00:00Z")));

        mockMvc.perform(get("/api/check-ins/images/21/url").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId").value(21))
                .andExpect(jsonPath("$.url").value(
                        "https://bucket.example.test/image?signature=redacted"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-15T04:00:00Z"));
    }

    @Test
    void requiresCsrfToken() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "body.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/check-ins/images/analyze")
                        .file(image)
                        .with(oidcLogin()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(imageAnalysisService);
    }
}
