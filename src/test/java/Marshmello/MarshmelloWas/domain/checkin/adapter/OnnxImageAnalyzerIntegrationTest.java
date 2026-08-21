package Marshmello.MarshmelloWas.domain.checkin.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.AnalysisResult;
import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.ImageAnalysisException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.analysis.onnx.enabled=true")
class OnnxImageAnalyzerIntegrationTest {

    @Autowired
    private ImageAnalyzer imageAnalyzer;

    @Test
    void selectsOnnxAdapterWhenPackagedModelsAreEnabled() {
        assertThat(imageAnalyzer).isInstanceOf(OnnxImageAnalyzer.class);
    }

    @Test
    void mapsUnsupportedImageBytesToInvalidImageReason() {
        assertThatThrownBy(() -> imageAnalyzer.analyze(new byte[]{1, 2, 3}))
                .isInstanceOfSatisfying(ImageAnalysisException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(ImageAnalysisException.Reason.INVALID_IMAGE));
    }

    @Test
    void runsPackagedLocatorAndUnetModels() throws IOException {
        AnalysisResult result = imageAnalyzer.analyze(pngBytes(checkerboardImage()));

        if (result.detected()) {
            assertThat(result.score()).isBetween((short) 1, (short) 8);
        } else {
            assertThat(result.score()).isNull();
        }
    }

    private BufferedImage checkerboardImage() {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int gray = ((x / 16) + (y / 16)) % 2 == 0 ? 64 : 192;
                image.setRGB(x, y, new Color(gray, gray, gray).getRGB());
            }
        }
        return image;
    }

    private byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
