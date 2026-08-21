package Marshmello.MarshmelloWas.domain.care.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.checkin.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.care.entity.CareCard;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedbackId;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGeneratedText;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGenerationException;
import Marshmello.MarshmelloWas.domain.care.repository.CareCardRepository;
import Marshmello.MarshmelloWas.domain.care.repository.UserActionFeedbackRepository;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CareCardIdentityHttpTest {

    private static final String SUBJECT = "care-user";
    private static final String NO_CARD_SUBJECT = "care-user-without-card";
    private static final String OTHER_SUBJECT = "care-other-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private CareCardRepository careCardRepository;

    @Autowired
    private UserActionFeedbackRepository feedbackRepository;

    @MockitoBean
    private CareCardGenerator generator;

    @Test
    void createsReadsAndRatesCurrentUsersCareCard() throws Exception {
        User user = userRepository.save(new User("care-owner", null));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("test", SUBJECT),
                user.getUserId()));
        CheckIn checkIn = checkInRepository.save(new CheckIn(
                false,
                LocalDate.of(2026, 8, 14),
                null,
                (short) 1,
                user.getUserId()));
        Image image = new Image(user.getUserId(), "test/http-care-image", "image/png", Instant.now());
        image.attachTo(checkIn, user.getUserId());
        imageRepository.save(image);
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), (short) 8));
        when(generator.generate(any())).thenReturn(new CareCardGeneratedText(
                "전문의 상담 신호 확인",
                "피부 이상 신호가 있으면 전문의와 상담하세요."));

        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", checkIn.id())
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkInId").value(checkIn.id()))
                .andExpect(jsonPath("$.actionName").value("전문의 상담 신호 확인"))
                .andExpect(jsonPath("$.source").isNotEmpty());

        mockMvc.perform(get("/api/check-ins/{checkInId}/care-card", checkIn.id())
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionReason").value("피부 이상 신호가 있으면 전문의와 상담하세요."));

        CareCard careCard = careCardRepository.findByCheckInId(checkIn.id()).orElseThrow();
        mockMvc.perform(get("/api/care-cards/latest")
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careCardId").value(careCard.getCareCardId()))
                .andExpect(jsonPath("$.checkInId").value(checkIn.id()))
                .andExpect(jsonPath("$.actionName").value("전문의 상담 신호 확인"))
                .andExpect(jsonPath("$.actionReason").value("피부 이상 신호가 있으면 전문의와 상담하세요."))
                .andExpect(jsonPath("$.category").value(careCard.getAction().getCategory()))
                .andExpect(jsonPath("$.source").value(careCard.getSource()))
                .andExpect(jsonPath("$.createdDate").value(careCard.getCreatedDate().toString()));

        mockMvc.perform(patch("/api/care-cards/{careCardId}/feedback", careCard.getCareCardId())
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"helpfulnessScore": 5}
                                """))
                .andExpect(status().isNoContent());

        UserActionFeedbackId feedbackId = new UserActionFeedbackId(
                user.getUserId(),
                careCard.getAction().getActionId());
        assertThat(feedbackRepository.findById(feedbackId).orElseThrow().getHelpfulnessScore())
                .isEqualTo((short) 5);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generationFailures")
    void returnsGranularErrorWhenCareCardGenerationFails(
            String reasonName,
            int expectedStatus,
            String expectedCode,
            boolean retryable
    ) throws Exception {
        User user = userRepository.save(new User("failure-owner", null));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("test", SUBJECT),
                user.getUserId()));
        CheckIn checkIn = checkInRepository.save(new CheckIn(
                false,
                LocalDate.of(2026, 8, 14),
                null,
                (short) 1,
                user.getUserId()));
        Image image = new Image(user.getUserId(), "test/http-care-failure", "image/png", Instant.now());
        image.attachTo(checkIn, user.getUserId());
        imageRepository.save(image);
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), (short) 8));
        when(generator.generate(any())).thenThrow(new CareCardGenerationException(
                CareCardGenerationException.Reason.valueOf(reasonName)));

        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", checkIn.id())
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT)))
                        .with(csrf()))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.retryable").value(retryable))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(careCardRepository.findByCheckInId(checkIn.id())).isEmpty();
    }

    private static Stream<Arguments> generationFailures() {
        return Stream.of(
                Arguments.of("UNAVAILABLE", 503, "AI_PROVIDER_UNAVAILABLE", true),
                Arguments.of("AUTHENTICATION", 502, "AI_PROVIDER_AUTHENTICATION_FAILED", false),
                Arguments.of("ACCESS_DENIED", 502, "AI_PROVIDER_ACCESS_DENIED", false),
                Arguments.of("MODEL_UNAVAILABLE", 502, "AI_MODEL_UNAVAILABLE", false),
                Arguments.of("QUOTA_EXCEEDED", 503, "AI_PROVIDER_QUOTA_EXCEEDED", false),
                Arguments.of("RATE_LIMITED", 503, "AI_PROVIDER_RATE_LIMITED", true),
                Arguments.of("REQUEST_REJECTED", 502, "AI_PROVIDER_REQUEST_REJECTED", false),
                Arguments.of("TIMEOUT", 504, "AI_GENERATION_TIMEOUT", true),
                Arguments.of("UPSTREAM", 502, "AI_PROVIDER_UPSTREAM_FAILURE", true),
                Arguments.of("INVALID_OUTPUT", 502, "AI_PROVIDER_INVALID_RESPONSE", true));
    }

    @Test
    void returnsCareCardNotFoundWhenCurrentUserHasNoCardButAnotherUserDoes() throws Exception {
        User userWithoutCard = userRepository.save(new User("care-empty", null));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("test", NO_CARD_SUBJECT),
                userWithoutCard.getUserId()));
        User otherUser = userRepository.save(new User("care-other-user", null));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("test", OTHER_SUBJECT),
                otherUser.getUserId()));
        CheckIn otherUsersCheckIn = checkInRepository.save(new CheckIn(
                false,
                LocalDate.of(2026, 8, 14),
                null,
                (short) 1,
                otherUser.getUserId()));
        Image image = new Image(otherUser.getUserId(), "test/http-other-care-image", "image/png", Instant.now());
        image.attachTo(otherUsersCheckIn, otherUser.getUserId());
        imageRepository.save(image);
        imageAnalysisRepository.save(new ImageAnalysis(image.id(), (short) 8));
        when(generator.generate(any())).thenReturn(new CareCardGeneratedText("other action", "other reason"));
        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", otherUsersCheckIn.id())
                        .with(oidcLogin().idToken(token -> token.subject(OTHER_SUBJECT)))
                        .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/care-cards/latest")
                        .with(oidcLogin().idToken(token -> token.subject(NO_CARD_SUBJECT))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARE_CARD_NOT_FOUND"));
    }
}
