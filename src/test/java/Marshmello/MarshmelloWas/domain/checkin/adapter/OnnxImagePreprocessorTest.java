package Marshmello.MarshmelloWas.domain.checkin.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class OnnxImagePreprocessorTest {

    private final OnnxImagePreprocessor preprocessor = new OnnxImagePreprocessor(4);

    @Test
    void rejectsUnsupportedImageBytes() {
        assertThatThrownBy(() -> preprocessor.prepare(new byte[]{1, 2, 3}))
                .isInstanceOf(IOException.class);
    }

    @Test
    void letterboxesAndNormalizesImageAsNchwTensor() throws IOException {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        image.setRGB(1, 0, Color.RED.getRGB());

        OnnxImagePreprocessor.PreparedImage prepared = preprocessor.prepare(pngBytes(image));

        assertThat(prepared.image().getWidth()).isEqualTo(2);
        assertThat(prepared.image().getHeight()).isEqualTo(1);
        assertThat(prepared.tensor().length).isEqualTo(1);
        assertThat(prepared.tensor()[0].length).isEqualTo(3);
        assertThat(prepared.tensor()[0][0]).hasDimensions(4, 4);
        assertThat(prepared.tensor()[0][0][0][0]).isCloseTo(
                (0.0f - 0.485f) / 0.229f,
                org.assertj.core.data.Offset.offset(0.0001f));
        assertThat(prepared.tensor()[0][0][1][0]).isCloseTo(
                (1.0f - 0.485f) / 0.229f,
                org.assertj.core.data.Offset.offset(0.0001f));
    }

    @Test
    void restoresSigmoidProbabilityToOriginalImageSize() throws IOException {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        OnnxImagePreprocessor.PreparedImage prepared = preprocessor.prepare(pngBytes(image));
        float[][] logits = new float[4][4];

        float[][] probability = prepared.restoreProbability(logits);

        assertThat(probability).hasDimensions(1, 2);
        assertThat(probability[0][0]).isCloseTo(
                0.5f,
                org.assertj.core.data.Offset.offset(0.0001f));
    }

    private byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
