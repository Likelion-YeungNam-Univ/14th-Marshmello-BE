package Marshmello.MarshmelloWas.domain.checkin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.checkin.dto.ImageAnalysisResponse;
import Marshmello.MarshmelloWas.domain.checkin.service.ImageAnalysisService;
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

    @Test
    void returnsFalseWhenDaveyScoreIsNotAvailable() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "body.png", "image/png", new byte[]{1, 2, 3});
        when(imageAnalysisService.analyze(any(byte[].class), eq("image/png")))
                .thenReturn(new ImageAnalysisResponse(false, null, null));

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
