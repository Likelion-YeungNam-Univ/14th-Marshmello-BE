package Marshmello.MarshmelloWas.domain.checkin.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.AnalysisResult;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage.ImageReadUrl;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage.StoredImage;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckInIdentityHttpTest {

    private static final String SUBJECT = "checkin-user";
    private static final Instant NOW = Instant.parse("2026-08-15T03:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @MockitoBean
    private ImageAnalyzer imageAnalyzer;

    @MockitoBean
    private ImageStorage imageStorage;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("checkin-user", null));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("test", SUBJECT),
                user.getUserId()));
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    @Test
    void connectsAnalysisCreationTimelineAndOriginalImageUrl() throws Exception {
        byte[] imageContent = {1, 2, 3};
        when(imageAnalyzer.analyze(any(byte[].class))).thenReturn(AnalysisResult.detected((short) 7));
        when(imageStorage.store(anyLong(), any(byte[].class), eq("image/png")))
                .thenReturn(new StoredImage("users/1/check-in-images/full-flow", "image/png"));
        when(imageStorage.createReadUrl("users/1/check-in-images/full-flow"))
                .thenReturn(new ImageReadUrl(
                        URI.create("https://bucket.example.test/full-flow?signature=redacted"),
                        Instant.parse("2026-08-15T03:10:00Z")));
        MockMultipartFile image = new MockMultipartFile(
                "image", "body.png", "image/png", imageContent);

        MvcResult analysis = mockMvc.perform(multipart("/api/check-ins/images/analyze")
                        .file(image)
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detected").value(true))
                .andExpect(jsonPath("$.score").doesNotExist())
                .andReturn();
        Number imageId = JsonPath.read(analysis.getResponse().getContentAsString(), "$.imageId");

        mockMvc.perform(post("/api/check-ins")
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageId": %d,
                                  "achieved": true,
                                  "diary": "오늘의 기록",
                                  "emotion": 3,
                                  "bodyDiaries": [
                                    {"bodyRegion": 2, "stretchMark": null, "comment": "복부"}
                                  ]
                                }
                                """.formatted(imageId.longValue())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageId").value(imageId.longValue()))
                .andExpect(jsonPath("$.checkInDate").value("2026-08-15"));

        mockMvc.perform(get("/api/check-ins")
                        .queryParam("date", "2026-08-15")
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageId").value(imageId.longValue()))
                .andExpect(jsonPath("$[0].achieved").value(true))
                .andExpect(jsonPath("$[0].diary").value("오늘의 기록"))
                .andExpect(jsonPath("$[0].emotion").value(3))
                .andExpect(jsonPath("$[0].bodyDiaries[0].bodyRegion").value(2))
                .andExpect(jsonPath("$[0].bodyDiaries[0].stretchMark").value(nullValue()))
                .andExpect(jsonPath("$[0].bodyDiaries[0].comment").value("복부"));

        mockMvc.perform(get("/api/check-ins/emotions")
                        .queryParam("month", "2026-08")
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-08-15"))
                .andExpect(jsonPath("$[0].emotion").value(3));

        mockMvc.perform(get("/api/check-ins/count")
                        .queryParam("month", "2026-08")
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.achievedCount").value(1));

        mockMvc.perform(get("/api/check-ins/body-diaries/top-region")
                        .queryParam("month", "2026-08")
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyRegion").value(2))
                .andExpect(jsonPath("$.count").doesNotExist());

        mockMvc.perform(get("/api/check-ins/images/{imageId}/url", imageId.longValue())
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(
                        "https://bucket.example.test/full-flow?signature=redacted"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-15T03:10:00Z"));
    }
}
