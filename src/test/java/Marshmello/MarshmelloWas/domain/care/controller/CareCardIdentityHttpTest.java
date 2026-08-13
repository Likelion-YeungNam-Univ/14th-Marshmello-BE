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

import Marshmello.MarshmelloWas.domain.analysis.entity.ImageAnalysis;
import Marshmello.MarshmelloWas.domain.analysis.repository.ImageAnalysisRepository;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.care.entity.CareCard;
import Marshmello.MarshmelloWas.domain.care.entity.UserActionFeedbackId;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator;
import Marshmello.MarshmelloWas.domain.care.port.CareCardGenerator.CareCardGeneratedText;
import Marshmello.MarshmelloWas.domain.care.repository.CareCardRepository;
import Marshmello.MarshmelloWas.domain.care.repository.UserActionFeedbackRepository;
import Marshmello.MarshmelloWas.domain.checkin.entity.CheckIn;
import Marshmello.MarshmelloWas.domain.checkin.entity.Image;
import Marshmello.MarshmelloWas.domain.checkin.repository.CheckInRepository;
import Marshmello.MarshmelloWas.domain.checkin.repository.ImageRepository;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
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
        Image image = imageRepository.save(new Image(new byte[]{1}, checkIn));
        imageAnalysisRepository.save(new ImageAnalysis(image.getImageId(), (short) 8));
        when(generator.generate(any())).thenReturn(new CareCardGeneratedText(
                "전문의 상담 신호 확인",
                "피부 이상 신호가 있으면 전문의와 상담하세요."));

        mockMvc.perform(post("/api/check-ins/{checkInId}/care-card", checkIn.getCheckInId())
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkInId").value(checkIn.getCheckInId()))
                .andExpect(jsonPath("$.actionName").value("전문의 상담 신호 확인"))
                .andExpect(jsonPath("$.source").isNotEmpty());

        mockMvc.perform(get("/api/check-ins/{checkInId}/care-card", checkIn.getCheckInId())
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionReason").value("피부 이상 신호가 있으면 전문의와 상담하세요."));

        CareCard careCard = careCardRepository.findByCheckInId(checkIn.getCheckInId()).orElseThrow();
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
}
