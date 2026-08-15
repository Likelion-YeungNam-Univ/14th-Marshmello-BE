package Marshmello.MarshmelloWas.domain.checkin.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccount;
import Marshmello.MarshmelloWas.domain.auth.entity.SocialAccountId;
import Marshmello.MarshmelloWas.domain.auth.repository.SocialAccountRepository;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageStorage;
import Marshmello.MarshmelloWas.domain.user.entity.User;
import Marshmello.MarshmelloWas.domain.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.analysis.onnx.enabled=true")
@AutoConfigureMockMvc
@Transactional
class CheckInOnnxImageHttpTest {

    private static final String SUBJECT = "onnx-checkin-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @MockitoBean
    private ImageStorage imageStorage;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("onnx-user", null));
        socialAccountRepository.save(new SocialAccount(
                new SocialAccountId("test", SUBJECT),
                user.getUserId()));
    }

    @Test
    void returnsFalseForImageThatCannotProduceDaveyScore() throws Exception {
        BufferedImage blank = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(blank, "png", output);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "blank.png",
                "image/png",
                output.toByteArray());

        mockMvc.perform(multipart("/api/check-ins/images/analyze")
                        .file(image)
                        .with(oidcLogin().idToken(token -> token.subject(SUBJECT)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detected").value(false))
                .andExpect(jsonPath("$.imageId").doesNotExist())
                .andExpect(jsonPath("$.score").doesNotExist());
        verifyNoInteractions(imageStorage);
    }
}
