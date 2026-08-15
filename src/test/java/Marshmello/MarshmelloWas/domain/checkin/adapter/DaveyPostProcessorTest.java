package Marshmello.MarshmelloWas.domain.checkin.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import Marshmello.MarshmelloWas.domain.checkin.port.ImageAnalyzer.AnalysisResult;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class DaveyPostProcessorTest {

    private static final int IMAGE_SIZE = 256;
    private final DaveyPostProcessor postProcessor = new DaveyPostProcessor();

    @Test
    void returnsNotDetectedWhenAbdomenAreaIsInvalid() {
        BufferedImage image = highContrastImage();
        float[][] abdomen = probabilityMap(new Rectangle(110, 110, 20, 20));

        AnalysisResult result = postProcessor.evaluate(
                image,
                predictions(
                        abdomen,
                        navelProbability(120, 120),
                        new float[IMAGE_SIZE][IMAGE_SIZE]));

        assertThat(result).isEqualTo(AnalysisResult.notDetected());
    }

    @Test
    void returnsNotDetectedWhenImageRequiresReview() {
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        float[][] abdomen = probabilityMap(new Rectangle(20, 20, 216, 216));

        AnalysisResult result = postProcessor.evaluate(
                image,
                predictions(
                        abdomen,
                        navelProbability(128, 128),
                        new float[IMAGE_SIZE][IMAGE_SIZE]));

        assertThat(result).isEqualTo(AnalysisResult.notDetected());
    }

    @Test
    void returnsNotDetectedWhenNoStretchMarkComponentExists() {
        BufferedImage image = highContrastImage();
        float[][] abdomen = probabilityMap(new Rectangle(20, 20, 216, 216));

        AnalysisResult result = postProcessor.evaluate(
                image,
                predictions(
                        abdomen,
                        navelProbability(128, 128),
                        new float[IMAGE_SIZE][IMAGE_SIZE]));

        assertThat(result).isEqualTo(AnalysisResult.notDetected());
    }

    @Test
    void returnsEightWhenEveryQuadrantContainsAtLeastFourComponents() {
        BufferedImage image = highContrastImage();
        float[][] abdomen = probabilityMap(new Rectangle(20, 20, 216, 216));
        float[][] stretch = new float[IMAGE_SIZE][IMAGE_SIZE];
        addComponents(stretch, new Rectangle(30, 30, 90, 90));
        addComponents(stretch, new Rectangle(136, 30, 90, 90));
        addComponents(stretch, new Rectangle(30, 136, 90, 90));
        addComponents(stretch, new Rectangle(136, 136, 90, 90));

        AnalysisResult result = postProcessor.evaluate(
                image,
                predictions(abdomen, navelProbability(128, 128), stretch));

        assertThat(result).isEqualTo(AnalysisResult.detected((short) 8));
    }

    private BufferedImage highContrastImage() {
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                int gray = ((x / 16) + (y / 16)) % 2 == 0 ? 64 : 192;
                image.setRGB(x, y, new Color(gray, gray, gray).getRGB());
            }
        }
        return image;
    }

    private float[][] probabilityMap(Rectangle rectangle) {
        float[][] map = new float[IMAGE_SIZE][IMAGE_SIZE];
        fillRectangle(map, rectangle, 1.0f);
        return map;
    }

    private float[][] navelProbability(int x, int y) {
        float[][] map = new float[IMAGE_SIZE][IMAGE_SIZE];
        map[y][x] = 1.0f;
        return map;
    }

    private DaveyPostProcessor.PredictionMaps predictions(
            float[][] abdomen,
            float[][] navel,
            float[][] stretch
    ) {
        return new DaveyPostProcessor.PredictionMaps(abdomen, navel, stretch);
    }

    private void addComponents(float[][] map, Rectangle quadrant) {
        for (int index = 0; index < 4; index++) {
            int y = quadrant.y + 8 + index * 18;
            fillRectangle(map, new Rectangle(quadrant.x + 8, y, 30, 4), 1.0f);
        }
    }

    private void fillRectangle(float[][] map, Rectangle rectangle, float value) {
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                map[y][x] = value;
            }
        }
    }
}
